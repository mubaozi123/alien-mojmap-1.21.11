package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.AmbientOcclusionEvent;
import dev.luminous.mod.modules.impl.player.PacketMine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class MixinAbstractBlock {
   @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
   private void onGetAmbientOcclusionLightLevel(BlockState state, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> info) {
      AmbientOcclusionEvent event = Alien.EVENT_BUS.post(AmbientOcclusionEvent.get());
      if (event.lightLevel != -1.0F) {
         info.setReturnValue(event.lightLevel);
      }
   }

   @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
   private void onComputeNextCollisionBox(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
      if (PacketMine.INSTANCE != null && pos.equals(PacketMine.getBreakPos()) && PacketMine.INSTANCE.noCollide.getValue() && PacketMine.ghost) {
         cir.setReturnValue(Shapes.empty());
      }
   }
}
