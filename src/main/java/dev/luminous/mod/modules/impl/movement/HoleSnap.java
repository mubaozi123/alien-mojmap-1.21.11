package dev.luminous.mod.modules.impl.movement;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.ImmediateDrawUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class HoleSnap extends Module {
   public static HoleSnap INSTANCE;
   public final BooleanSetting any = this.add(new BooleanSetting("AnyHole", true));
   public final SliderSetting timer = this.add(new SliderSetting("Timer", 1.0, 0.1, 8.0, 0.1));
   public final BooleanSetting up = this.add(new BooleanSetting("Up", true));
   public final BooleanSetting grim = this.add(new BooleanSetting("Grim", false));
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
   public final SliderSetting circleSize = this.add(new SliderSetting("CircleSize", 1.0, 0.1F, 2.5));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true));
   public final SliderSetting segments = this.add(new SliderSetting("Segments", 180, 0, 360));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 1, 50));
   private final SliderSetting timeoutTicks = this.add(new SliderSetting("TimeOut", 40, 0, 100));
   private final SliderSetting steps = this.add(new SliderSetting("Steps", 0.8, 0.0, 1.0, 0.01, this.grim::getValue));
   private final SliderSetting priority = this.add(new SliderSetting("Priority", 10, 0, 100, this.grim::getValue));
   boolean resetMove = false;
   boolean applyTimer = false;
   Vec3 targetPos;
   private BlockPos holePos;
   private int stuckTicks;
   private int enabledTicks;

   public HoleSnap() {
      super("HoleSnap", "HoleSnap", Module.Category.Movement);
      this.setChinese("拉坑");
      INSTANCE = this;
   }

   public static Vec2 getRotationTo(Vec3 posFrom, Vec3 posTo) {
      Vec3 vec3d = posTo.subtract(posFrom);
      return getRotationFromVec(vec3d);
   }

   public static void drawCircle(PoseStack matrixStack, Color color, double circleSize, Vec3 pos, int segments) {
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 camPos = camera.position();
      Matrix4f matrix = matrixStack.last().pose();
      float a = color.getAlpha() / 255.0F;
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      Vec3 center = pos.add(-camPos.x, -camPos.y, -camPos.z);
      bufferBuilder.addVertex(matrix, (float)center.x, (float)center.y, (float)center.z).setColor(r, g, b, a);
      double i = 0.0;

      while (i <= 360.0) {
         double rad = Math.toRadians(i);
         double x = Math.sin(rad) * circleSize;
         double z = Math.cos(rad) * circleSize;
         Vec3 tempPos = new Vec3(pos.x + x, pos.y, pos.z + z).add(-camPos.x, -camPos.y, -camPos.z);
         bufferBuilder.addVertex(matrix, (float)tempPos.x, (float)tempPos.y, (float)tempPos.z).setColor(r, g, b, a);
         i += 360.0 / segments;
      }

      ImmediateDrawUtil.drawWorld(bufferBuilder, true);
   }

   private static Vec2 getRotationFromVec(Vec3 vec) {
      double d = vec.x;
      double d2 = vec.z;
      double xz = Math.hypot(d, d2);
      d2 = vec.z;
      double d3 = vec.x;
      double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - 90.0);
      double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
      return new Vec2((float)yaw, (float)pitch);
   }

   private static double normalizeAngle(double angleIn) {
      double angle = angleIn % 360.0;
      if (angle >= 180.0) {
         angle -= 360.0;
      }

      if (angle < -180.0) {
         angle += 360.0;
      }

      return angle;
   }

   @EventListener(priority = -99)
   public void onTimer(TimerEvent event) {
      if (this.applyTimer) {
         event.set(this.timer.getValueFloat());
      }
   }

   @Override
   public void onEnable() {
      this.applyTimer = false;
      if (nullCheck()) {
         this.disable();
      } else {
         this.resetMove = false;
         this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      }
   }

   @Override
   public void onDisable() {
      this.holePos = null;
      this.stuckTicks = 0;
      this.enabledTicks = 0;
      if (!nullCheck()) {
         if (this.resetMove && !this.grim.getValue()) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
         }
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.disable();
      }
   }

   @EventListener(priority = -999)
   public void onKeyInput(KeyboardInputEvent e) {
      if (this.grim.getValue()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            this.sendMessage("§4HoleSnap require MovementSync.");
            this.disable();
         } else if (!mc.player.isHandsBusy() && !Freecam.INSTANCE.isOn()) {
            MovementUtil.setMovement(mc.player, 1.0F, 0.0F);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.holePos = Alien.HOLE.getHole((float)this.range.getValue(), true, this.any.getValue(), this.up.getValue());
      if (this.holePos == null) {
         this.disable();
      } else {
         this.enabledTicks++;
         if (this.enabledTicks > this.timeoutTicks.getValue() - 1.0) {
            this.disable();
         } else {
            this.applyTimer = true;
            if (this.grim.getValue()) {
               if (!mc.player.isAlive() || mc.player.isFallFlying()) {
                  this.disable();
               } else if (this.stuckTicks > 8) {
                  this.disable();
               } else if (this.holePos == null) {
                  this.disable();
               } else {
                  Vec3 playerPos = mc.player.position();
                  this.targetPos = new Vec3(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
                  Direction facing;
                  if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
                     this.targetPos = this.targetPos
                        .add(new Vec3(facing.getUnitVec3i().getX() * 0.5, facing.getUnitVec3i().getY() * 0.5, facing.getUnitVec3i().getZ() * 0.5));
                  }

                  this.applyTimer = true;
                  this.resetMove = true;
                  float rotation = getRotationTo(playerPos, this.targetPos).x;
                  float yawRad = rotation / 180.0F * (float) Math.PI;
                  double dist = playerPos.distanceTo(this.targetPos);
                  double cappedSpeed = Math.min(0.2873, dist);
                  double x = -((float)Math.sin(yawRad)) * cappedSpeed;
                  double z = (float)Math.cos(yawRad) * cappedSpeed;
                  if (Math.abs(x) < 0.25 && Math.abs(z) < 0.25 && playerPos.y <= this.holePos.getY() + 0.8) {
                     this.disable();
                  } else {
                     this.stuckTicks = mc.player.horizontalCollision ? ++this.stuckTicks : 0;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.grim.getValue() && this.holePos != null) {
         this.targetPos = new Vec3(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
         Direction facing;
         if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
            this.targetPos = this.targetPos
               .add(new Vec3(facing.getUnitVec3i().getX() * 0.5, facing.getUnitVec3i().getY() * 0.5, facing.getUnitVec3i().getZ() * 0.5));
         }

         event.setTarget(this.targetPos, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onMove(MoveEvent event) {
      if (!this.grim.getValue()) {
         if (!mc.player.isAlive() || mc.player.isFallFlying()) {
            this.disable();
         } else if (this.stuckTicks > 8) {
            this.disable();
         } else if (this.holePos == null) {
            this.disable();
         } else {
            Vec3 playerPos = mc.player.position();
            this.targetPos = new Vec3(this.holePos.getX() + 0.5, mc.player.getY(), this.holePos.getZ() + 0.5);
            Direction facing;
            if (Alien.HOLE.isDoubleHole(this.holePos) && (facing = Alien.HOLE.is3Block(this.holePos)) != null) {
               this.targetPos = this.targetPos
                  .add(new Vec3(facing.getUnitVec3i().getX() * 0.5, facing.getUnitVec3i().getY() * 0.5, facing.getUnitVec3i().getZ() * 0.5));
            }

            this.applyTimer = true;
            this.resetMove = true;
            float rotation = getRotationTo(playerPos, this.targetPos).x;
            float yawRad = rotation / 180.0F * (float) Math.PI;
            double dist = playerPos.distanceTo(this.targetPos);
            double cappedSpeed = Math.min(0.2873, dist);
            double x = -((float)Math.sin(yawRad)) * cappedSpeed;
            double z = (float)Math.cos(yawRad) * cappedSpeed;
            event.setX(x);
            event.setZ(z);
            if (Math.abs(x) < 0.1 && Math.abs(z) < 0.1 && playerPos.y <= this.holePos.getY() + 0.5) {
               this.disable();
            }

            this.stuckTicks = mc.player.horizontalCollision ? ++this.stuckTicks : 0;
         }
      }
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (this.targetPos != null && this.holePos != null) {
         GL11.glEnable(3042);
         Color color = this.color.getValue();
         Vec3 pos = new Vec3(this.targetPos.x, this.holePos.getY(), this.targetPos.z());
         if (this.fade.getValue()) {
            double temp = 0.01;

            for (double i = 0.0; i < this.circleSize.getValue(); i += temp) {
               drawCircle(
                  matrixStack,
                  ColorUtil.injectAlpha(color, (int)Math.min(color.getAlpha() * 2 / (this.circleSize.getValue() / temp), 255.0)),
                  i,
                  pos,
                  this.segments.getValueInt()
               );
            }
         } else {
            drawCircle(matrixStack, color, this.circleSize.getValue(), pos, this.segments.getValueInt());
         }

         GL11.glDisable(3042);
      }
   }
}
