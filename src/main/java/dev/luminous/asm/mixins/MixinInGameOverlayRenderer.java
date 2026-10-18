package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {
   @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
   private static void onRenderFireOverlay(PoseStack matrices, MultiBufferSource vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fireOverlay.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
   private static void onRenderUnderwaterOverlay(Minecraft client, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.waterOverlay.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
   private static void onrenderInWallOverlay(TextureAtlasSprite sprite, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.blockOverlay.getValue()) {
         ci.cancel();
      }
   }
}
