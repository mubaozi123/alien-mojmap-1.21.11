package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Ambience;
import java.awt.Color;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeProbe.class)
public class MixinEnvironmentAttributeInterpolator {
   @Inject(method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
   private void hookGetEnvironmentAttribute(EnvironmentAttribute environmentAttribute, float f, CallbackInfoReturnable<Object> cir) {
      Ambience ambience = Ambience.INSTANCE;
      if (ambience != null && ambience.isOn()) {
         Color awtColor = null;
         if (environmentAttribute == EnvironmentAttributes.SKY_COLOR && ambience.sky.booleanValue) {
            awtColor = ambience.sky.getValue();
         } else if (environmentAttribute == EnvironmentAttributes.CLOUD_COLOR && ambience.cloud.booleanValue) {
            awtColor = ambience.cloud.getValue();
         } else if (environmentAttribute == EnvironmentAttributes.FOG_COLOR && ambience.fog.booleanValue) {
            awtColor = ambience.fog.getValue();
         } else if (environmentAttribute == EnvironmentAttributes.SKY_LIGHT_COLOR && ambience.worldColor.booleanValue) {
            awtColor = ambience.worldColor.getValue();
         }

         if (awtColor != null) {
            cir.setReturnValue(awtColor.getRGB());
         }
      }
   }
}
