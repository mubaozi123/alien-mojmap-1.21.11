package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.JumpEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AntiEffects;
import dev.luminous.mod.modules.impl.render.ViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
   @Final
   @Shadow
   private static AttributeModifier SPEED_MODIFIER_SPRINTING;
   @Unique
   private boolean previousElytra = false;
   @Unique
   private long lastLerp = 0L;

   public MixinLivingEntity(EntityType<?> type, Level world) {
      super(type, world);
   }

   @Shadow
   @Nullable
   public AttributeInstance getAttribute(Holder<Attribute> attribute) {
      return this.getAttributes().getInstance(attribute);
   }

   @Shadow
   public AttributeMap getAttributes() {
      return null;
   }

   @Shadow
   @Override
   public abstract void remove(RemovalReason var1);

   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void getArmSwingAnimationEnd(CallbackInfoReturnable<Integer> info) {
      if (ViewModel.INSTANCE.isOn() && ViewModel.INSTANCE.slowAnimation.getValue()) {
         info.setReturnValue(ViewModel.INSTANCE.slowAnimationVal.getValueInt());
      }
   }

   @Redirect(
      method = "getEffectiveGravity",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"),
      require = 0
   )
   private boolean slowFallHook(LivingEntity instance, Holder<MobEffect> effect) {
      return AntiEffects.INSTANCE.isOn() && effect == MobEffects.SLOW_FALLING && AntiEffects.INSTANCE.slowFalling.getValue()
         ? false
         : instance.hasEffect(effect);
   }

   @Redirect(
      method = "travelInAir",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"
      ),
      require = 0
   )
   private MobEffectInstance levitationHook(LivingEntity instance, Holder<MobEffect> effect) {
      return AntiEffects.INSTANCE.isOn() && effect == MobEffects.LEVITATION && AntiEffects.INSTANCE.levitation.getValue() ? null : instance.getEffect(effect);
   }

   @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
   private void jumpPre(CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (self instanceof Player player) {
         JumpEvent pre = JumpEvent.get(Event.Stage.Pre);
         Alien.EVENT_BUS.post(pre);
         if (pre.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "jumpFromGround", at = @At("RETURN"))
   private void jumpPost(CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (self instanceof Player) {
         JumpEvent post = JumpEvent.get(Event.Stage.Post);
         Alien.EVENT_BUS.post(post);
      }
   }

   @Redirect(
      method = "handleRelativeFrictionAndCalculateMovement",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onClimbable()Z"),
      require = 0
   )
   public boolean climbingHook(LivingEntity instance) {
      return Velocity.INSTANCE.isOn() && Velocity.INSTANCE.noClimb.getValue() && LivingEntity.class.cast(this) == Minecraft.getInstance().player
         ? false
         : instance.onClimbable();
   }

   @Redirect(method = "handleOnClimbable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onClimbable()Z"), require = 0)
   public boolean climbingHook2(LivingEntity instance) {
      return NoSlow.INSTANCE.climb() && LivingEntity.class.cast(this) == Minecraft.getInstance().player ? false : instance.onClimbable();
   }

   @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
   public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
      if (LivingEntity.class.cast(this) == Minecraft.getInstance().player) {
         SprintEvent event = SprintEvent.get();
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            sprinting = event.isSprint();
            super.setSprinting(sprinting);
            AttributeInstance entityAttributeInstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            entityAttributeInstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
            if (sprinting) {
               entityAttributeInstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
            }
         }
      }
   }
}
