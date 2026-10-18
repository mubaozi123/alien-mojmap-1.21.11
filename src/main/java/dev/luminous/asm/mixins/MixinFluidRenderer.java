package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.luminous.mod.modules.impl.render.Xray;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public abstract class MixinFluidRenderer {
   @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
   private void onRender(BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo info) {
      if (Xray.shouldBlock(fluidState.createLegacyBlock())) {
         info.cancel();
      }
   }
}
