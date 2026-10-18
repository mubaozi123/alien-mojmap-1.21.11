package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.FireworkShooterRotationEvent;
import dev.luminous.api.events.impl.RemoveFireworkEvent;
import dev.luminous.api.utils.Wrapper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity implements Wrapper {
   @Shadow
   private int life;

   @Inject(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;updateRotation()V", shift = Shift.AFTER),
      cancellable = true
   )
   private void hookTickPre(CallbackInfo ci) {
      FireworkRocketEntity rocketEntity = FireworkRocketEntity.class.cast(this);
      RemoveFireworkEvent removeFireworkEvent = RemoveFireworkEvent.get(rocketEntity);
      Alien.EVENT_BUS.post(removeFireworkEvent);
      if (removeFireworkEvent.isCancelled()) {
         ci.cancel();
         if (this.life == 0 && !rocketEntity.isSilent()) {
            mc.level
               .playSound(
                  null, rocketEntity.getX(), rocketEntity.getY(), rocketEntity.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F
               );
         }

         this.life++;
         if (mc.level.isClientSide) {
            mc.level
               .addParticle(
                  ParticleTypes.FIREWORK,
                  rocketEntity.getX(),
                  rocketEntity.getY(),
                  rocketEntity.getZ(),
                  mc.level.random.nextGaussian() * 0.05,
                  -rocketEntity.getDeltaMovement().y * 0.5,
                  mc.level.random.nextGaussian() * 0.05
               );
         }
      }
   }

   @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"),
      require = 0
   )
   public Vec3 hook(LivingEntity instance) {
      FireworkShooterRotationEvent event = FireworkShooterRotationEvent.get(instance, instance.getYRot(), instance.getXRot());
      Alien.EVENT_BUS.post(event);
      return event.isCancelled() ? event.getRotationVector() : instance.getLookAngle();
   }
}
