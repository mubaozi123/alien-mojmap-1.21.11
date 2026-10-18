package dev.luminous.asm.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.luminous.mod.modules.impl.render.Xray;
import java.util.List;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class MixinBlockModelRenderer {
   @Inject(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At("HEAD"), cancellable = true)
   private void onRenderSmooth(
      BlockAndTintGetter world,
      List<BlockModelPart> model,
      BlockState state,
      BlockPos pos,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      boolean cull,
      int overlay,
      CallbackInfo info
   ) {
      if (Xray.shouldBlock(state)) {
         info.cancel();
      }
   }

   @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
   private static boolean hookShouldDrawFace(boolean original, BlockAndTintGetter world, BlockState state, boolean cull, Direction side, BlockPos pos) {
      Xray xray = Xray.INSTANCE;
      return xray.isOn() ? xray.modifyDrawSideAtNeighbor(state, world, pos, side, original) : original;
   }
}
