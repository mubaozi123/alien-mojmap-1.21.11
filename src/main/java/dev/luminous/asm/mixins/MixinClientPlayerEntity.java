package dev.luminous.asm.mixins;

import com.mojang.authlib.GameProfile;
import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.MovedEvent;
import dev.luminous.api.events.impl.SendMovementPacketsEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.TickMovementEvent;
import dev.luminous.asm.accessors.IClientPlayerEntity;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.exploit.PacketControl;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {
   @Shadow
   public ClientInput input;
   @Final
   @Shadow
   protected Minecraft minecraft;
   @Shadow
   private int positionReminder;
   @Shadow
   private double xLast;
   @Shadow
   private double yLast;
   @Shadow
   private double zLast;
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;
   @Unique
   private float alien$preYaw;
   @Unique
   private float alien$prePitch;
   @Unique
   private boolean alien$rotating;

   public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
   private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
      if (Velocity.INSTANCE.isOn() && Velocity.INSTANCE.blockPush.getValue()) {
         info.cancel();
      }
   }

   @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), require = 0)
   private boolean tickMovementHook(LocalPlayer player) {
      return NoSlow.INSTANCE.noSlow() ? false : player.isUsingItem();
   }

   @Inject(at = @At("HEAD"), method = "handlePortalTransitionEffect", cancellable = true)
   private void updateNausea(CallbackInfo ci) {
      if (ClientSetting.INSTANCE.portalGui()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "move",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
      ),
      cancellable = true
   )
   public void onMoveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
      MoveEvent event = MoveEvent.get(movement.x, movement.y, movement.z);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.modify) {
         ci.cancel();
         super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
         Alien.EVENT_BUS.post(MovedEvent.INSTANCE);
      }
   }

   @Inject(method = "move", at = @At("TAIL"))
   public void onMoveReturnHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
      Alien.EVENT_BUS.post(MovedEvent.INSTANCE);
   }

   @Shadow
   @Override
   public abstract float getViewXRot(float var1);

   @Shadow
   @Override
   public abstract float getViewYRot(float var1);

   @Inject(method = "tick", at = @At("HEAD"))
   private void tickHead(CallbackInfo ci) {
      try {
         Alien.EVENT_BUS.post(TickEvent.get(Event.Stage.Pre));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void tickReturn(CallbackInfo ci) {
      try {
         Alien.EVENT_BUS.post(TickEvent.get(Event.Stage.Post));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (ClientPlayerEntity.tick() [RETURN]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(method = "sendPosition", at = @At("HEAD"))
   private void onSendMovementPacketsHead(CallbackInfo info) {
      this.alien$applyRotation();
      if (PacketControl.INSTANCE.isOn()
         && PacketControl.INSTANCE.positionSync.getValue()
         && this.positionReminder >= PacketControl.INSTANCE.positionDelay.getValueInt() - 1) {
         ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
      }

      if (RotationManager.snapBack) {
         ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
         ((IClientPlayerEntity)this).setLastYaw(999.0F);
         RotationManager.snapBack = false;
      } else {
         if (AntiCheat.INSTANCE.fullPackets.getValue()) {
            double d = this.getX() - this.xLast;
            double e = this.getY() - this.yLast;
            double f = this.getZ() - this.zLast;
            double g = this.getYRot() - this.yRotLast;
            double h = this.getXRot() - this.xRotLast;
            boolean bl3 = g != 0.0 || h != 0.0;
            if (AntiCheat.INSTANCE.force.getValue() || !(Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4)) && this.positionReminder >= 19 || bl3) {
               ((IClientPlayerEntity)this).setTicksSinceLastPositionPacketSent(50);
               ((IClientPlayerEntity)this).setLastYaw(999.0F);
            }
         }
      }
   }

   @Inject(method = "sendPosition", at = @At("TAIL"))
   private void onSendMovementPacketsTail(CallbackInfo info) {
      this.alien$restoreRotation();
   }

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
   private float alien$redirectVehicleYaw(LocalPlayer instance) {
      if (instance.isPassenger()) {
         this.alien$applyRotation();
      }

      return instance.getYRot();
   }

   @Inject(
      method = "tick",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
         ordinal = 1,
         shift = Shift.AFTER
      )
   )
   private void onTickVehicleAfterSendPackets(CallbackInfo info) {
      this.alien$restoreRotation();
   }

   @Unique
   private void alien$applyRotation() {
      if (!this.alien$rotating) {
         float currentYaw = this.getYRot();
         float currentPitch = this.getXRot();
         SendMovementPacketsEvent event = SendMovementPacketsEvent.get(currentYaw, currentPitch);
         Alien.EVENT_BUS.post(event);
         Alien.ROTATION.rotationYaw = event.getYaw();
         Alien.ROTATION.rotationPitch = event.getPitch();
         if (Float.compare(currentYaw, event.getYaw()) != 0 || Float.compare(currentPitch, event.getPitch()) != 0) {
            this.alien$preYaw = currentYaw;
            this.alien$prePitch = currentPitch;
            this.alien$rotating = true;
            this.setYRot(event.getYaw());
            this.setXRot(event.getPitch());
         }
      }
   }

   @Unique
   private void alien$restoreRotation() {
      if (this.alien$rotating) {
         this.setYRot(this.alien$preYaw);
         this.setXRot(this.alien$prePitch);
         this.alien$rotating = false;
      }
   }

   @Inject(method = "aiStep", at = @At("HEAD"))
   private void tickMovement(CallbackInfo ci) {
      Alien.EVENT_BUS.post(TickMovementEvent.INSTANCE);
   }
}
