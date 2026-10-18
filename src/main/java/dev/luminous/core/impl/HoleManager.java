package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.impl.combat.Breaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class HoleManager implements Wrapper {
   public boolean isHole(BlockPos pos) {
      return this.isHole(pos, true, false, false);
   }

   public boolean isHole(BlockPos pos, boolean canStand, boolean checkTrap, boolean anyBlock) {
      int blockProgress = 0;

      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN && (anyBlock && !mc.level.isEmptyBlock(pos.relative(i)) || Alien.HOLE.isHard(pos.relative(i)))) {
            blockProgress++;
         }
      }

      return (
            !checkTrap
               || mc.level.isEmptyBlock(pos)
                  && mc.level.isEmptyBlock(pos.above())
                  && mc.level.isEmptyBlock(pos.above(1))
                  && mc.level.isEmptyBlock(pos.above(2))
                  && (mc.player.getBlockY() - 1 <= pos.getY() || mc.level.isEmptyBlock(pos.above(3)))
                  && (mc.player.getBlockY() - 2 <= pos.getY() || mc.level.isEmptyBlock(pos.above(4)))
         )
         && blockProgress > 3
         && (!canStand || BlockUtil.canCollide(new AABB(pos.offset(0, -1, 0))));
   }

   public BlockPos getHole(float range, boolean doubleHole, boolean any, boolean up) {
      BlockPos bestPos = null;
      double bestDistance = range + 1.0F;

      for (BlockPos pos : BlockUtil.getSphere(range, mc.player.position())) {
         if ((pos.getX() == mc.player.getBlockX() && pos.getZ() == mc.player.getBlockZ() || up || !(pos.getY() + 1 > mc.player.getY()))
            && (Alien.HOLE.isHole(pos, true, true, any) || doubleHole && this.isDoubleHole(pos))
            && pos.getY() - mc.player.getBlockY() <= 1) {
            double getDistance = Mth.sqrt((float)mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (bestPos == null || getDistance < bestDistance) {
               bestPos = pos;
               bestDistance = getDistance;
            }
         }
      }

      return bestPos;
   }

   public boolean isDoubleHole(BlockPos pos) {
      Direction unHardFacing = this.is3Block(pos);
      return unHardFacing != null ? this.is3Block(pos.relative(unHardFacing)) != null : false;
   }

   public Direction is3Block(BlockPos pos) {
      if (!this.isHard(pos.below())) {
         return null;
      } else if (mc.level.isEmptyBlock(pos) && mc.level.isEmptyBlock(pos.above()) && mc.level.isEmptyBlock(pos.above(2))) {
         int progress = 0;
         Direction unHardFacing = null;

         for (Direction facing : Direction.values()) {
            if (facing != Direction.UP && facing != Direction.DOWN) {
               if (this.isHard(pos.relative(facing))) {
                  progress++;
               } else {
                  int progress2 = 0;

                  for (Direction facing2 : Direction.values()) {
                     if (facing2 != Direction.DOWN && facing2 != facing.getOpposite() && this.isHard(pos.relative(facing).relative(facing2))) {
                        progress2++;
                     }
                  }

                  if (progress2 == 4) {
                     progress++;
                  } else {
                     unHardFacing = facing;
                  }
               }
            }
         }

         return progress == 3 ? unHardFacing : null;
      } else {
         return null;
      }
   }

   public boolean isHard(BlockPos pos) {
      Block block = mc.level.getBlockState(pos).getBlock();
      return this.isHard(block);
   }

   public boolean isHard(Block block) {
      return block == Blocks.BEDROCK || Breaker.hard.contains(block);
   }
}
