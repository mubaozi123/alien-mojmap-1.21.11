package dev.luminous.asm.accessors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
   @Accessor("lastHurtByMobTimestamp")
   int getLastAttackedTicks();

   @Accessor("noJumpDelay")
   int getLastJumpCooldown();

   @Accessor("noJumpDelay")
   void setLastJumpCooldown(int var1);

   @Accessor("swimAmount")
   float getLeaningPitch();

   @Accessor("swimAmount")
   void setLeaningPitch(float var1);

   @Accessor("swimAmountO")
   void setLastLeaningPitch(float var1);
}
