package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.RenderBlockEntityEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinBlockEntityRenderDispatcher {
   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private <S extends BlockEntityRenderState> void onRenderEntity(
      S renderState, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraRenderState, CallbackInfo info
   ) {
      RenderBlockEntityEvent event = Alien.EVENT_BUS.post(RenderBlockEntityEvent.get(renderState.blockState));
      if (event.isCancelled()) {
         info.cancel();
      }
   }
}
