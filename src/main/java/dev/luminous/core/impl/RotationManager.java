package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DoAttackEvent;
import dev.luminous.api.events.impl.FireworkShooterRotationEvent;
import dev.luminous.api.events.impl.InteractBlockEvent;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.api.events.impl.JumpEvent;
import dev.luminous.api.events.impl.MovementInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.SendMovementPacketsEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.TickMovementEvent;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.asm.accessors.IClientPlayerEntity;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.movement.HoleSnap;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.enums.SnapBack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationManager implements Wrapper {
   public static final Timer ROTATE_TIMER = new Timer();
   public static Vec3 directionVec = null;
   public static boolean snapBack = false;
   private static float renderPitch;
   private static float renderYawOffset;
   private static float prevRenderPitch;
   private static float prevRenderYawOffset;
   private static float prevRotationYawHead;
   private static float rotationYawHead;
   public float nextYaw;
   public float nextPitch;
   public float rotationYaw;
   public float rotationPitch;
   public float lastYaw;
   public float lastPitch;
   public Vec3 crossHairUpdatePos;
   private int ticksExisted;
   public static float fixYaw;
   public static float fixPitch;
   private float prevYaw;
   private float prevPitch;

   public RotationManager() {
      Alien.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onInteract(InteractItemEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYRot(), mc.player.getXRot());
         } else {
            this.snapBack();
         }
      }
   }

   @EventListener
   public void onInteract(InteractBlockEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYRot(), mc.player.getXRot());
         } else {
            this.snapBack();
         }
      }
   }

   @EventListener
   public void doAttack(DoAttackEvent event) {
      if (AntiCheat.INSTANCE.interactRotation.getValue() && mc.player != null) {
         if (event.isPre()) {
            this.snapAt(mc.player.getYRot(), mc.player.getXRot());
         } else {
            this.snapBack();
         }
      }
   }

   public void snapBack() {
      if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Force)) {
         mc.getConnection()
            .send(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), this.rotationYaw, this.rotationPitch, mc.player.onGround(), false));
      } else if (AntiCheat.INSTANCE.snapBackEnum.is(SnapBack.Tick)) {
         snapBack = true;
      }
   }

   public void lookAt(Vec3 directionVec) {
      this.rotationTo(directionVec);
      this.snapAt(directionVec);
   }

   public void lookAt(BlockPos pos, Direction side) {
      Vec3 hitVec = pos.getCenter().add(new Vec3(side.getUnitVec3i().getX() * 0.5, side.getUnitVec3i().getY() * 0.5, side.getUnitVec3i().getZ() * 0.5));
      this.lookAt(hitVec);
   }

   public void snapAt(float yaw, float pitch) {
      this.setRenderRotation(yaw, pitch, true);
      if (AntiCheat.INSTANCE.grimRotation.getValue()) {
         mc.getConnection().send(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), false));
      } else {
         mc.getConnection().send(new Rot(yaw, pitch, mc.player.onGround(), false));
      }
   }

   public void snapAt(Vec3 directionVec) {
      float[] angle = getRotation(directionVec);
      this.snapAt(angle[0], angle[1]);
   }

   public void rotationTo(Vec3 vec3d) {
      ROTATE_TIMER.reset();
      directionVec = vec3d;
   }

   public boolean inFov(Vec3 directionVec, float fov) {
      float[] angle = getRotation(
         this.crossHairUpdatePos != null
            ? this.crossHairUpdatePos
            : new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()),
         directionVec
      );
      return this.inFov(angle[0], angle[1], fov);
   }

   public boolean inFov(float yaw, float pitch, float fov) {
      float yawDifferent = Mth.degreesDifferenceAbs(yaw, this.rotationYaw);
      float pitchDifferent;
      return yawDifferent * yawDifferent + (pitchDifferent = Math.abs(pitch - this.rotationPitch)) * pitchDifferent <= fov * fov;
   }

   @EventListener
   public void onTickMovement(TickMovementEvent event) {
      if (mc.player != null) {
         this.crossHairUpdatePos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
      }
   }

   @EventListener
   public void update(SendMovementPacketsEvent event) {
      if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
         event.setYaw(this.nextYaw);
         event.setPitch(this.nextPitch);
      } else {
         UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(event.getYaw(), event.getPitch());
         Alien.EVENT_BUS.post(updateRotateEvent);
         event.setYaw(updateRotateEvent.getYaw());
         event.setPitch(updateRotateEvent.getPitch());
      }
   }

   @EventListener(priority = 999)
   public void update(TickMovementEvent event) {
      if (mc.player != null) {
         if (AntiCheat.INSTANCE.movementSync() && !BaritoneUtil.isActive()) {
            UpdateRotateEvent updateRotateEvent = UpdateRotateEvent.get(mc.player.getYRot(), mc.player.getXRot());
            Alien.EVENT_BUS.post(updateRotateEvent);
            this.nextYaw = updateRotateEvent.getYaw();
            this.nextPitch = updateRotateEvent.getPitch();
            fixYaw = this.nextYaw;
            fixPitch = this.nextPitch;
         }
      }
   }

   @EventListener(priority = -200)
   public void onLastRotation(UpdateRotateEvent event) {
      RotationEvent rotationEvent = RotationEvent.get();
      Alien.EVENT_BUS.post(rotationEvent);
      if (rotationEvent.getRotation()) {
         float[] newAngle = this.injectStep(new float[]{rotationEvent.getYaw(), rotationEvent.getPitch()}, rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (rotationEvent.getTarget() != null) {
         float[] newAngle = this.injectStep(rotationEvent.getTarget(), rotationEvent.getSpeed());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      } else if (!event.isModified()
         && AntiCheat.INSTANCE.look.getValue()
         && directionVec != null
         && !ROTATE_TIMER.passed((long)(AntiCheat.INSTANCE.rotateTime.getValue() * 1000.0))) {
         float[] newAngle = this.injectStep(directionVec, AntiCheat.INSTANCE.steps.getValueFloat());
         event.setYaw(newAngle[0]);
         event.setPitch(newAngle[1]);
      }
   }

   @EventListener
   public void travel(TravelEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isHandsBusy()) {
               if (e.isPre()) {
                  this.lastYaw = mc.player.getYRot();
                  this.lastPitch = mc.player.getXRot();
                  mc.player.setYRot(fixYaw);
                  mc.player.setXRot(fixPitch);
               } else {
                  mc.player.setYRot(this.lastYaw);
                  mc.player.setXRot(this.lastPitch);
               }
            }
         }
      }
   }

   @EventListener
   public void onJump(JumpEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!mc.player.isHandsBusy()) {
               if (e.isPre()) {
                  this.lastYaw = mc.player.getYRot();
                  this.lastPitch = mc.player.getXRot();
                  mc.player.setYRot(fixYaw);
                  mc.player.setXRot(fixPitch);
               } else {
                  mc.player.setYRot(this.lastYaw);
                  mc.player.setXRot(this.lastPitch);
               }
            }
         }
      }
   }

   @EventListener
   public void onFirework(FireworkShooterRotationEvent event) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (event.shooter == mc.player) {
               event.yaw = fixYaw;
               event.pitch = fixPitch;
               event.cancel();
            }
         }
      }
   }

   @EventListener(priority = -999)
   public void onKeyInput(MovementInputEvent e) {
      if (AntiCheat.INSTANCE.movementSync()) {
         if (!BaritoneUtil.isActive()) {
            if (!HoleSnap.INSTANCE.isOn()) {
               if (!mc.player.isHandsBusy() && !Freecam.INSTANCE.isOn()) {
                  float mF = MovementUtil.getMovementForward();
                  float mS = MovementUtil.getMovementSideways();
                  float delta = (mc.player.getYRot() - fixYaw) * (float) (Math.PI / 180.0);
                  float cos = Mth.cos(delta);
                  float sin = Mth.sin(delta);
                  MovementUtil.setMovement(mc.player, Math.round(mF * cos + mS * sin), Math.round(mS * cos - mF * sin));
               }
            }
         }
      }
   }

   public float[] injectStep(Vec3 vec, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = Mth.wrapDegrees(
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - 90.0) - currentYaw
      );
      float pitchDelta = (float)(
            -Math.toDegrees(
               Math.atan2(
                  vec.y - (mc.player.position().y + mc.player.getEyeHeight(mc.player.getPose())),
                  Math.sqrt(Math.pow(vec.x - mc.player.getX(), 2.0) + Math.pow(vec.z - mc.player.getZ(), 2.0))
               )
            )
         )
         - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians(27 * (mc.player.tickCount % 30));
         yawDelta = (float)(yawDelta + Math.sin(angleToRad) * 3.0) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float clampedYawDelta = Mth.clamp(Mth.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = Mth.clamp(pitchDelta, -45.0F, 45.0F);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = Mth.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      return new float[]{newYaw, newPitch};
   }

   public float[] injectStep(float[] angle, float steps) {
      float currentYaw = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastYaw() : this.rotationYaw;
      float currentPitch = AntiCheat.INSTANCE.serverSide.getValue() ? this.getLastPitch() : this.rotationPitch;
      float yawDelta = Mth.wrapDegrees(angle[0] - currentYaw);
      float pitchDelta = angle[1] - currentPitch;
      if (AntiCheat.INSTANCE.random.getValue()) {
         float angleToRad = (float)Math.toRadians(27 * (mc.player.tickCount % 30));
         yawDelta = (float)(yawDelta + Math.sin(angleToRad) * 3.0) + MathUtil.random(-1.0F, 1.0F);
         pitchDelta += MathUtil.random(-0.6F, 0.6F);
      }

      if (yawDelta > 180.0F) {
         yawDelta -= 180.0F;
      }

      float yawStepVal = 180.0F * steps;
      float pitchStepVal = 90.0F * steps;
      float clampedYawDelta = Mth.clamp(Mth.abs(yawDelta), -yawStepVal, yawStepVal);
      float clampedPitchDelta = Mth.clamp(pitchDelta, -pitchStepVal, pitchStepVal);
      float newYaw = currentYaw + (yawDelta > 0.0F ? clampedYawDelta : -clampedYawDelta);
      float newPitch = Mth.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);
      return new float[]{newYaw, newPitch};
   }

   @EventListener(priority = -999)
   public void onPacketSend(PacketEvent.Sent event) {
      if (mc.player != null) {
         Packet<?> packet2 = event.getPacket();
         ServerboundMovePlayerPacket packet;
         if (packet2 instanceof ServerboundMovePlayerPacket && (packet = (ServerboundMovePlayerPacket)packet2).hasRotation()) {
            this.setLastYaw(packet.getYRot(this.getLastYaw()));
            this.setLastPitch(packet.getXRot(this.getLastPitch()));
            this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), ClientSetting.INSTANCE.sync.getValue());
         }
      }
   }

   @EventListener
   public void onUpdateWalkingPost(TickEvent event) {
      if (event.isPost()) {
         this.setRenderRotation(this.getLastYaw(), this.getLastPitch(), false);
      }
   }

   public void setRenderRotation(float yaw, float pitch, boolean force) {
      if (mc.player != null) {
         if (mc.player.tickCount != this.ticksExisted || force) {
            this.ticksExisted = mc.player.tickCount;
            prevRenderPitch = renderPitch;
            prevRenderYawOffset = renderYawOffset;
            renderYawOffset = this.getRenderYawOffset(yaw, prevRenderYawOffset);
            prevRotationYawHead = rotationYawHead;
            rotationYawHead = yaw;
            renderPitch = pitch;
         }
      }
   }

   private float getRenderYawOffset(float yaw, float offsetIn) {
      float result = offsetIn;
      double xDif = mc.player.getX() - mc.player.xOld;
      double zDif;
      if (xDif * xDif + (zDif = mc.player.getZ() - mc.player.zOld) * zDif > 0.0025000002F) {
         float offset = (float)Mth.atan2(zDif, xDif) * (180.0F / (float)Math.PI) - 90.0F;
         float wrap = Mth.abs(Mth.wrapDegrees(yaw) - offset);
         result = 95.0F < wrap && wrap < 265.0F ? offset - 180.0F : offset;
      }

      if (mc.player.attackAnim > 0.0F) {
         result = yaw;
      }

      float offset;
      if ((offset = Mth.wrapDegrees(yaw - (result = offsetIn + Mth.wrapDegrees(result - offsetIn) * 0.3F))) < -75.0F) {
         offset = -75.0F;
      } else if (offset >= 75.0F) {
         offset = 75.0F;
      }

      result = yaw - offset;
      if (offset * offset > 2500.0F) {
         result += offset * 0.2F;
      }

      return result;
   }

   public static float[] getRotation(Vec3 eyesPos, Vec3 vec) {
      double diffX = vec.x - eyesPos.x;
      double diffY = vec.y - eyesPos.y;
      double diffZ = vec.z - eyesPos.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
   }

   public static float[] getRotation(Vec3 vec) {
      Vec3 eyesPos = mc.player.getEyePosition();
      return getRotation(eyesPos, vec);
   }

   public static float getRenderPitch() {
      return renderPitch;
   }

   public static float getRotationYawHead() {
      return rotationYawHead;
   }

   public static float getRenderYawOffset() {
      return renderYawOffset;
   }

   public static float getPrevRenderPitch() {
      return prevRenderPitch;
   }

   public static float getPrevRotationYawHead() {
      return prevRotationYawHead;
   }

   public static float getPrevRenderYawOffset() {
      return prevRenderYawOffset;
   }

   public float getLastYaw() {
      return this.lastYaw;
   }

   public void setLastYaw(float lastYaw) {
      this.lastYaw = lastYaw;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Alien.SERVER.playerNull.passedS(0.15)) {
         ((IClientPlayerEntity)mc.player).setLastYaw(lastYaw);
      }
   }

   public float getLastPitch() {
      return this.lastPitch;
   }

   public void setLastPitch(float lastPitch) {
      this.lastPitch = lastPitch;
      if (AntiCheat.INSTANCE.forceSync.getValue() && Alien.SERVER.playerNull.passedS(0.15)) {
         mc.player.xRotO = lastPitch;
      }
   }
}
