package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.player.FreeLook;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.render.CameraClip;
import dev.luminous.mod.modules.impl.render.MotionCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class MixinCamera {
   @Shadow
   private boolean detached;

   @Shadow
   protected abstract float getMaxZoom(float var1);

   @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V", ordinal = 0))
   private void modifyCameraDistance(Args args) {
      if (CameraClip.INSTANCE.isOn()) {
         args.set(0, -this.getMaxZoom((float)CameraClip.INSTANCE.getDistance()));
      }
   }

   @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
   private void onClipToSpace(float f, CallbackInfoReturnable<Float> cir) {
      if (CameraClip.INSTANCE.isOn()) {
         cir.setReturnValue((float)CameraClip.INSTANCE.getDistance());
      }
   }

   @Inject(method = "setup", at = @At("TAIL"))
   private void updateHook(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      if (Freecam.INSTANCE.isOn()) {
         this.detached = true;
      }
   }

   @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
   private void setRotationHook(Args args) {
      if (Freecam.INSTANCE.isOn()) {
         args.setAll(new Object[]{Freecam.INSTANCE.getFakeYaw(), Freecam.INSTANCE.getFakePitch()});
      } else if (FreeLook.INSTANCE.isOn()) {
         args.setAll(new Object[]{FreeLook.INSTANCE.getFakeYaw(), FreeLook.INSTANCE.getFakePitch()});
      }
   }

   @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
   private void setPosHook(Args args) {
      if (Freecam.INSTANCE.isOn()) {
         args.setAll(new Object[]{Freecam.INSTANCE.getFakeX(), Freecam.INSTANCE.getFakeY(), Freecam.INSTANCE.getFakeZ()});
      } else if (MotionCamera.INSTANCE.on()) {
         args.setAll(new Object[]{MotionCamera.INSTANCE.getFakeX(), MotionCamera.INSTANCE.getFakeY(), MotionCamera.INSTANCE.getFakeZ()});
      }
   }
}
