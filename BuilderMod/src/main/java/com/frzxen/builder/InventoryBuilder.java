package com.frzxen.builder;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class InventoryBuilder implements ClientModInitializer {
 private Plan plan;
 private List<Plan.Step> steps=List.of();
 private int cursor=0, delay=0;
 private boolean building=false;
 private String prompt="";
 private static void say(String s){MinecraftClient mc=MinecraftClient.getInstance();if(mc.player!=null)mc.player.sendMessage(Text.literal("§b[Builder] §f"+s),false);}
 @Override public void onInitializeClient(){
  ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(literal("builder")
   .then(literal("create").then(argument("description",StringArgumentType.greedyString()).executes(c->{
    prompt=StringArgumentType.getString(c,"description");plan=Plan.generate(prompt);steps=List.of();building=false;
    say("Created a local template for: "+prompt+" ("+plan.size()+" blocks). Run /builder preview.");return 1;
   })))
   .then(literal("preview").executes(c->{
    MinecraftClient mc=MinecraftClient.getInstance();if(plan==null||mc.player==null){say("Create a plan first.");return 0;}
    BlockPos origin=mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(),4);
    steps=plan.positioned(origin);cursor=0;building=false;
    say("Plan anchored at "+origin.toShortString()+". "+steps.size()+" blocks. /builder materials then /builder start.");return 1;
   }))
   .then(literal("materials").executes(c->{
    if(plan==null){say("Create a plan first.");return 0;}
    plan.materials().forEach((block,count)->say(block.getName().getString()+": "+count));return 1;
   }))
   .then(literal("start").executes(c->{if(steps.isEmpty()){say("Preview a plan first.");return 0;}building=true;say("Building started. Stay close to the next block; keep materials in your hotbar.");return 1;}))
   .then(literal("pause").executes(c->{building=false;say("Paused at "+cursor+"/"+steps.size());return 1;}))
   .then(literal("status").executes(c->{say("Progress "+cursor+"/"+steps.size()+" | "+(building?"running":"paused"));return 1;}))
   .then(literal("cancel").executes(c->{building=false;steps=List.of();cursor=0;say("Plan cleared.");return 1;}))
  ));
  ClientTickEvents.END_CLIENT_TICK.register(this::tick);
 }
 private void tick(MinecraftClient mc){
  if(!building||mc.player==null||mc.world==null||mc.interactionManager==null||steps.isEmpty())return;
  if(++delay<5)return;delay=0;
  if(cursor>=steps.size()){building=false;say("Build complete!");return;}
  Plan.Step step=steps.get(cursor);
  BlockPos target=step.pos();
  BlockState existing=mc.world.getBlockState(target);
  if(existing.isOf(step.block())){cursor++;return;}
  if(!existing.isReplaceable()){building=false;say("Paused: occupied block at "+target.toShortString());return;}
  if(mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(target))>20.25){building=false;say("Paused: move closer to "+target.toShortString()+" and /builder start.");return;}
  int slot=-1;
  for(int i=0;i<9;i++){
   ItemStack stack=mc.player.getInventory().getStack(i);
   if(stack.getItem() instanceof BlockItem bi && bi.getBlock()==step.block()){slot=i;break;}
  }
  if(slot<0){building=false;say("Paused: put "+step.block().getName().getString()+" in your hotbar, then /builder start.");return;}
  // Require a real solid adjacent face. Never place through the world or use creative commands.
  Direction face=null;BlockPos support=null;
  for(Direction d:new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,Direction.UP}){
   BlockPos adjacent=target.offset(d);
   if(!mc.world.getBlockState(adjacent).isAir() && mc.world.getBlockState(adjacent).isSideSolidFullSquare(mc.world,adjacent,d.getOpposite())){face=d.getOpposite();support=adjacent;break;}
  }
  if(face==null){building=false;say("Paused: no support face for "+target.toShortString()+". Add scaffolding/support manually.");return;}
  mc.player.getInventory().setSelectedSlot(slot);
  Vec3d hit=Vec3d.ofCenter(support).add(Vec3d.of(face.getVector()).multiply(0.5));
  BlockHitResult result=new BlockHitResult(hit,face,support,false);
  ActionResult action=mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,result);
  if(action.isAccepted()){mc.player.swingHand(Hand.MAIN_HAND);cursor++;}
  else {building=false;say("Paused: server rejected placement at "+target.toShortString());}
 }
}
