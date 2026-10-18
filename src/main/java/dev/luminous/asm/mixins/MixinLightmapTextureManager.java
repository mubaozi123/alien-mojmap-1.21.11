package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LightmapGammaEvent;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LightTexture.class)
public abstract class MixinLightmapTextureManager {
   @Shadow
   private boolean updateLightTexture;
   @Shadow
   private float blockLightRedFlicker;
   @Final
   @Shadow
   private GameRenderer renderer;
   @Final
   @Shadow
   private Minecraft minecraft;

   @Inject(
      method = "updateLightTexture",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;calculateDarknessScale(Lnet/minecraft/world/entity/LivingEntity;FF)F"),
      cancellable = true
   )
   private void hookNoRenderDarkness(float tickProgress, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.darkness.getValue()) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 4)
   private float modifyDarknessFactor(float value) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.darkness.getValue() ? 0.0F : value;
   }

   @ModifyVariable(method = "updateLightTexture", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
   private Vector3f modifyWorldColorVector(Vector3f vector3f) {
      if (Ambience.INSTANCE.isOn()) {
         int argb = Ambience.INSTANCE.worldColor.getValue().getRGB();
         float r = (argb >> 16 & 0xFF) / 255.0F;
         float g = (argb >> 8 & 0xFF) / 255.0F;
         float b = (argb & 0xFF) / 255.0F;
         return new Vector3f(r, g, b);
      } else {
         return vector3f;
      }
   }

   @ModifyArgs(
      method = "updateLightTexture(F)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 6)
   )
   private void hookLightmapGamma(Args args) {
      float originalGamma = (Float)args.get(0);
      LightmapGammaEvent event = new LightmapGammaEvent(originalGamma);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         args.set(0, event.getGamma());
      }
   }

   @Shadow
   private float calculateDarknessScale(LivingEntity entity, float factor, float tickProgress) {
      return 0.0F;
   }

   @Shadow
   public static float getBrightness(float ambientLight, int lightLevel) {
      return 0.0F;
   }
}
