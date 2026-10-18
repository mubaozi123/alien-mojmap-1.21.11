package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LerpToEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InterpolationHandler.class)
public abstract class MixinPositionInterpolator {
   @Shadow
   private Entity entity;
   private long lastLerp = 0L;

   @Inject(method = "interpolateTo", at = @At("HEAD"))
   private void lerpToHook(Vec3 pow, float yaw, float pitch, CallbackInfo ci) {
      if (this.entity instanceof LivingEntity livingEntity) {
         Alien.EVENT_BUS.post(LerpToEvent.get(livingEntity, pow.x, pow.y, pow.z, yaw, pitch, this.lastLerp));
         this.lastLerp = System.currentTimeMillis();
      }
   }
}
