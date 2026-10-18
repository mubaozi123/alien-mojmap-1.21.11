package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.LookDirectionEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class MixinEntity {
   @Shadow
   private static final int FLAG_SPRINTING = 3;

   @Shadow
   private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
      return null;
   }

   @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
   private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (Entity.class.cast(this) == Wrapper.mc.player) {
         LookDirectionEvent lookDirectionEvent = LookDirectionEvent.get(Entity.class.cast(this), cursorDeltaX, cursorDeltaY);
         Alien.EVENT_BUS.post(lookDirectionEvent);
         if (lookDirectionEvent.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(at = @At("HEAD"), method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z", cancellable = true)
   private void onIsInvisibleCheck(Player message, CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.invisible.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
   void isGlowingHook(CallbackInfoReturnable<Boolean> cir) {
      if (ShaderModule.INSTANCE.isOn()) {
         cir.setReturnValue(ShaderModule.INSTANCE.shouldRender(Entity.class.cast(this)));
      }
   }

   @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
   private void pushAwayFromHook(Args args) {
      if (Entity.class.cast(this) == Minecraft.getInstance().player && Velocity.INSTANCE.isOn() && Velocity.INSTANCE.entityPush.getValue()) {
         args.set(0, 0.0);
         args.set(1, 0.0);
         args.set(2, 0.0);
      }
   }

   @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
   void isOnFireHook(CallbackInfoReturnable<Boolean> cir) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fireEntity.getValue()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
   public void setSprintingHook(boolean sprinting, CallbackInfo ci) {
      if (Entity.class.cast(this) == Minecraft.getInstance().player) {
         SprintEvent event = SprintEvent.get();
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            sprinting = event.isSprint();
            this.setSharedFlag(3, sprinting);
         }
      }
   }

   @Inject(method = "getBlockSpeedFactor()F", at = @At("RETURN"), cancellable = true)
   private void setCallbackInfoReturnable2(CallbackInfoReturnable callbackInfoReturnable) {
      if ((Object)this instanceof LocalPlayer && NoSlow.INSTANCE != null && NoSlow.INSTANCE.isOn()) {
         callbackInfoReturnable.setReturnValue(1.0F);
      }
   }

   @Shadow
   protected void setSharedFlag(int index, boolean value) {
   }
}
