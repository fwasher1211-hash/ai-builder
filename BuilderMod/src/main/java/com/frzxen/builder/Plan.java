package com.frzxen.builder;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public final class Plan {
 public record Step(BlockPos pos, Block block) {}
 private final LinkedHashMap<BlockPos,Block> blocks = new LinkedHashMap<>();
 private void put(int x,int y,int z,Block b){blocks.put(new BlockPos(x,y,z),b);}
 private void fill(int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int y=y1;y<=y2;y++)for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)put(x,y,z,b);}
 public static Plan generate(String prompt){
  String p=prompt.toLowerCase(Locale.ROOT); Plan a=new Plan();
  boolean castle=p.contains("castle")||p.contains("fortress")||p.contains("tower");
  boolean cabin=p.contains("cabin")||p.contains("wood")||p.contains("cottage");
  boolean large=p.contains("large")||p.contains("huge")||p.contains("massive");
  int r=large?7:5;
  Block wall=castle?Blocks.STONE_BRICKS:cabin?Blocks.SPRUCE_PLANKS:Blocks.OAK_PLANKS;
  Block roof=castle?Blocks.DEEPSLATE_TILES:cabin?Blocks.DARK_OAK_PLANKS:Blocks.SPRUCE_PLANKS;
  Block floor=castle?Blocks.COBBLESTONE:Blocks.OAK_PLANKS;
  a.fill(-r,0,-r,r,0,r,floor);
  for(int y=1;y<=5;y++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(Math.abs(x)==r||Math.abs(z)==r)a.put(x,y,z,wall);
  // Doorway, two-block-high opening
  a.blocks.remove(new BlockPos(0,1,-r));a.blocks.remove(new BlockPos(0,2,-r));
  // Window openings
  for(int y=2;y<=3;y++)for(int s:new int[]{-1,1}){a.blocks.remove(new BlockPos(s*r,y,0));a.blocks.remove(new BlockPos(0,y,r));}
  if(castle){
   a.fill(-r-1,6,-r-1,r+1,6,r+1,roof);
   for(int x=-r-1;x<=r+1;x++)for(int z=-r-1;z<=r+1;z++)if(Math.abs(x)==r+1||Math.abs(z)==r+1){if((x+z)%2==0)a.put(x,7,z,wall);}
   for(int sx:new int[]{-r,r})for(int sz:new int[]{-r,r}){
    for(int y=1;y<=9;y++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(Math.abs(dx)==1||Math.abs(dz)==1)a.put(sx+dx,y,sz+dz,wall);
    a.fill(sx-2,10,sz-2,sx+2,10,sz+2,roof);
   }
  }else{
   for(int y=6;y<=9;y++){int inset=y-6; a.fill(-r+inset,y,-r+inset,r-inset,y,r-inset,roof);}
   for(int x:new int[]{-r,r})for(int z:new int[]{-r,r})for(int y=1;y<=5;y++)a.put(x,y,z,cabin?Blocks.STRIPPED_SPRUCE_LOG:Blocks.OAK_LOG);
  }
  return a;
 }
 public List<Step> positioned(BlockPos origin){List<Step> result=new ArrayList<>();for(var e:blocks.entrySet())result.add(new Step(origin.add(e.getKey()),e.getValue()));result.sort(Comparator.comparingInt(s->s.pos().getY()));return result;}
 public Map<Block,Integer> materials(){Map<Block,Integer> counts=new LinkedHashMap<>();blocks.values().forEach(b->counts.merge(b,1,Integer::sum));return counts;}
 public int size(){return blocks.size();}
}
