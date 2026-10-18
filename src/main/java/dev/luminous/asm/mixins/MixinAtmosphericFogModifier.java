package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.Xray;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AtmosphericFogEnvironment.class)
public class MixinAtmosphericFogModifier {
   private static final float SAFE_FAR = 102400.0F;

   @Inject(
      method = "setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V",
      at = @At("TAIL")
   )
   private void applyFogSettings(FogData fogData, Camera camera, ClientLevel clientWorld, float f, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
      if ((!NoRender.INSTANCE.isOn() || !NoRender.INSTANCE.fog.getValue()) && !Xray.INSTANCE.isOn()) {
         Ambience ambience = Ambience.INSTANCE;
         if (ambience.isOn() && ambience.fogDistance.getValue()) {
            float customStart = ambience.fogStart.getValueFloat();
            float customEnd = ambience.fogEnd.getValueFloat();
            if (customStart >= customEnd || customEnd > 102400.0F) {
               customStart = 0.0F;
               customEnd = 128.0F;
            }

            fogData.environmentalStart = customStart;
            fogData.environmentalEnd = customEnd;
         }
      } else {
         fogData.environmentalStart = 0.0F;
         fogData.environmentalEnd = 102400.0F;
         fogData.renderDistanceStart = 0.0F;
         fogData.renderDistanceEnd = 102400.0F;
         fogData.skyEnd = 102400.0F;
         fogData.cloudEnd = 102400.0F;
      }
   }
}
