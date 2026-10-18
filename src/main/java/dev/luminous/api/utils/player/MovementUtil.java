package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.asm.accessors.IInput;
import dev.luminous.asm.accessors.IVec3d;
import dev.luminous.mod.modules.impl.movement.HoleSnap;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

public class MovementUtil implements Wrapper {
   public static boolean isMoving() {
      Input pi = mc.player.input.keyPresses;
      return pi.forward() || pi.backward() || pi.left() || pi.right() || HoleSnap.INSTANCE.isOn();
   }

   public static boolean isStatic() {
      return mc.player.getDeltaMovement().x() == 0.0 && mc.player.onGround() && mc.player.getDeltaMovement().z() == 0.0;
   }

   public static boolean isJumping() {
      return mc.player.input.keyPresses.jump();
   }

   public static float getMovementForward() {
      Input pi = mc.player.input.keyPresses;
      return (pi.forward() ? 1.0F : 0.0F) - (pi.backward() ? 1.0F : 0.0F);
   }

   public static float getMovementSideways() {
      Input pi = mc.player.input.keyPresses;
      return (pi.left() ? 1.0F : 0.0F) - (pi.right() ? 1.0F : 0.0F);
   }

   public static void setMovement(Player player, float forward, float sideways) {
      ClientInput input = ((LocalPlayer)player).input;
      Input pi = input.keyPresses;
      input.keyPresses = new Input(forward > 0.0F, forward < 0.0F, sideways > 0.0F, sideways < 0.0F, pi.jump(), pi.shift(), pi.sprint());
      setMovementVector(player, forward, sideways);
   }

   public static void setMovementVector(Player player, float forward, float sideways) {
      Vec2 vector = new Vec2(sideways, forward);
      if (vector.lengthSquared() > 0.0F) {
         vector = vector.normalized();
      }

      ((IInput)((LocalPlayer)player).input).setMovementVector(vector);
   }

   public static void setSneaking(Player player, boolean sneaking) {
      Input pi = ((LocalPlayer)player).input.keyPresses;
      ((LocalPlayer)player).input.keyPresses = new Input(pi.forward(), pi.backward(), pi.left(), pi.right(), pi.jump(), sneaking, pi.sprint());
   }

   public static void resetInput(Player player) {
      ((LocalPlayer)player).input.keyPresses = Input.EMPTY;
   }

   public static double getDistance2D() {
      double xDist = mc.player.getX() - mc.player.xOld;
      double zDist = mc.player.getZ() - mc.player.zOld;
      return Math.sqrt(xDist * xDist + zDist * zDist);
   }

   public static double getJumpSpeed() {
      double defaultSpeed = 0.0;
      if (mc.player.hasEffect(MobEffects.JUMP_BOOST)) {
         int amplifier = mc.player.getActiveEffectsMap().get(MobEffects.JUMP_BOOST).getAmplifier();
         defaultSpeed += (amplifier + 1) * 0.1;
      }

      return defaultSpeed;
   }

   public static double[] directionSpeed(double speed) {
      float forward = getMovementForward();
      float side = getMovementSideways();
      float yaw = mc.player.yRotO + (mc.player.getYRot() - mc.player.yRotO) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      return directionSpeed(speed, yaw, forward, side);
   }

   public static double[] directionSpeed(double speed, float yaw) {
      float forward = getMovementForward();
      float side = getMovementSideways();
      return directionSpeed(speed, yaw, forward, side);
   }

   private static double[] directionSpeed(double speed, float yaw, float forward, float side) {
      if (forward != 0.0F) {
         if (side > 0.0F) {
            yaw += forward > 0.0F ? -45 : 45;
         } else if (side < 0.0F) {
            yaw += forward > 0.0F ? 45 : -45;
         }

         side = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double posX = forward * speed * cos + side * speed * sin;
      double posZ = forward * speed * sin - side * speed * cos;
      return new double[]{posX, posZ};
   }

   public static double getMotionX() {
      return mc.player.getDeltaMovement().x;
   }

   public static void setMotionX(double x) {
      ((IVec3d)mc.player.getDeltaMovement()).setX(x);
   }

   public static double getMotionY() {
      return mc.player.getDeltaMovement().y;
   }

   public static void setMotionY(double y) {
      ((IVec3d)mc.player.getDeltaMovement()).setY(y);
   }

   public static double getMotionZ() {
      return mc.player.getDeltaMovement().z;
   }

   public static void setMotionZ(double z) {
      ((IVec3d)mc.player.getDeltaMovement()).setZ(z);
   }

   public static double getSpeed(boolean slowness) {
      double defaultSpeed = 0.2873;
      return getSpeed(slowness, defaultSpeed);
   }

   public static double getSpeed(boolean slowness, double defaultSpeed) {
      if (mc.player.hasEffect(MobEffects.SPEED)) {
         int amplifier = mc.player.getActiveEffectsMap().get(MobEffects.SPEED).getAmplifier();
         defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
      }

      if (slowness && mc.player.hasEffect(MobEffects.SLOWNESS)) {
         int amplifier = mc.player.getActiveEffectsMap().get(MobEffects.SLOWNESS).getAmplifier();
         defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
      }

      if (mc.player.isShiftKeyDown()) {
         defaultSpeed /= 5.0;
      }

      return defaultSpeed;
   }
}
