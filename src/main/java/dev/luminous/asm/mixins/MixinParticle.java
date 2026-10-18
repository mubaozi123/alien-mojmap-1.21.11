package dev.luminous.asm.mixins;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class MixinParticle {
   @Shadow
   protected double xd;
   @Shadow
   protected double yd;
   @Shadow
   protected double zd;

   public abstract void setColor(float var1, float var2, float var3);

   protected void setAlpha(float alpha) {
   }
}
