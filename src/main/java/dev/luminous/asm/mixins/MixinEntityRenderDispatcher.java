package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.RenderEntityEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   public void onRender(
      EntityRenderState renderState,
      CameraRenderState cameraState,
      double offsetX,
      double offsetY,
      double offsetZ,
      PoseStack matrices,
      SubmitNodeCollector queue,
      CallbackInfo ci
   ) {
      RenderEntityEvent event = RenderEntityEvent.get(renderState.entityType);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }
}
