package dev.luminous.api.events.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public class PlaceBlockEvent {
   private static final PlaceBlockEvent INSTANCE = new PlaceBlockEvent();
   public BlockPos blockPos;
   public Block block;

   private PlaceBlockEvent() {
   }

   public static PlaceBlockEvent get(BlockPos blockPos, Block block) {
      INSTANCE.blockPos = blockPos;
      INSTANCE.block = block;
      return INSTANCE;
   }
}
