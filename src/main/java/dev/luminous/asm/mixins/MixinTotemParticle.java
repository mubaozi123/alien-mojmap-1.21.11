package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.TotemParticleEvent;
import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemParticle.class)
public abstract class MixinTotemParticle extends MixinParticle {
   @Inject(method = "<init>", at = @At("TAIL"))
   private void hookInit(
      ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet spriteProvider, CallbackInfo ci
   ) {
      TotemParticleEvent event = TotemParticleEvent.get(velocityX, velocityY, velocityZ);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         this.xd = event.velocityX;
         this.yd = event.velocityY;
         this.zd = event.velocityZ;
         Color color = event.color;
         if (color != null) {
            this.setColor(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
            this.setAlpha(color.getAlpha() / 255.0F);
         }
      }
   }
}
