package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.world.level.block.state.BlockState;

public class RenderBlockEntityEvent extends Event {
   private static final RenderBlockEntityEvent INSTANCE = new RenderBlockEntityEvent();
   public BlockState blockState;

   public static RenderBlockEntityEvent get(BlockState blockState) {
      INSTANCE.setCancelled(false);
      INSTANCE.blockState = blockState;
      return INSTANCE;
   }
}
