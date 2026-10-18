package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PlaceBlockEvent;
import dev.luminous.mod.modules.Module;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
   @Inject(method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("RETURN"))
   private void onPlace(@NotNull BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> info) {
      if (!Module.nullCheck()) {
         if (context.getLevel().isClientSide) {
            Alien.EVENT_BUS.post(PlaceBlockEvent.get(context.getClickedPos(), state.getBlock()));
         }
      }
   }
}
