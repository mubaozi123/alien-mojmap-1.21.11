package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BlindnessFogEnvironment.class)
public class MixinBlindnessEffectFogModifier {
   @Inject(
      method = "setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V",
      at = @At("TAIL")
   )
   private void removeBlindnessFog(FogData fogData, Camera camera, ClientLevel clientWorld, float f, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.blindness.getValue()) {
         float far = 1.0E9F;
         fogData.environmentalStart = far;
         fogData.environmentalEnd = far;
         fogData.renderDistanceStart = far;
         fogData.renderDistanceEnd = far;
         fogData.skyEnd = far;
         fogData.cloudEnd = far;
      }
   }
}
