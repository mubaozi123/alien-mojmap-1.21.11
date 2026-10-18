package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TimerEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class FastFall extends Module {
   private final EnumSetting<FastFall.Mode> mode = this.add(new EnumSetting<>("Mode", FastFall.Mode.Fast));
   private final BooleanSetting noLag = this.add(new BooleanSetting("NoLag", true, () -> this.mode.getValue() == FastFall.Mode.Fast));
   private final BooleanSetting useTimerSetting = this.add(new BooleanSetting("UseTimer", false));
   private final SliderSetting timer = this.add(new SliderSetting("Timer", 2.5, 1.0, 8.0, 0.1, this.useTimerSetting::getValue));
   private final BooleanSetting anchor = this.add(new BooleanSetting("Anchor", true));
   private final SliderSetting height = this.add(new SliderSetting("Height", 10.0, 1.0, 20.0, 0.5));
   private final Timer lagTimer = new Timer();
   boolean onGround = false;
   private boolean useTimer;

   public FastFall() {
      super("FastFall", "Miyagi son simulator", Module.Category.Movement);
      this.setChinese("快速坠落");
   }

   @Override
   public void onDisable() {
      this.useTimer = false;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener(priority = -100)
   public void onMove(MoveEvent event) {
      if (!nullCheck()) {
         if (mc.player.onGround() && this.anchor.getValue() && this.traceDown() != 0 && this.traceDown() <= this.height.getValue() && this.trace()) {
            event.setX(event.getX() * 0.05);
            event.setZ(event.getZ() * 0.05);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if ((!(this.height.getValue() > 0.0) || !(this.traceDown() > this.height.getValue()))
         && !mc.player.isInWall()
         && !mc.player.isUnderWater()
         && !mc.player.isInLava()
         && !mc.player.isSuppressingSlidingDownLadder()
         && this.lagTimer.passed(1000L)
         && !mc.player.isFallFlying()
         && !Fly.INSTANCE.isOn()
         && !nullCheck()) {
         if (!Alien.PLAYER.isInWeb(mc.player)) {
            if (mc.player.onGround() && this.mode.getValue() == FastFall.Mode.Fast) {
               MovementUtil.setMotionY(MovementUtil.getMotionY() - (this.noLag.getValue() ? 0.62F : 1.0F));
            }

            if (this.useTimerSetting.getValue()) {
               if (!mc.player.onGround()) {
                  if (this.onGround) {
                     this.useTimer = true;
                  }

                  if (MovementUtil.getMotionY() >= 0.0) {
                     this.useTimer = false;
                  }

                  this.onGround = false;
               } else {
                  this.useTimer = false;
                  MovementUtil.setMotionY(-0.08);
                  this.onGround = true;
               }
            } else {
               this.useTimer = false;
            }
         }
      }
   }

   @EventListener
   public void onTimer(TimerEvent event) {
      if (!nullCheck()) {
         if (!mc.player.onGround() && this.useTimer) {
            event.set(this.timer.getValueFloat());
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!nullCheck() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.lagTimer.reset();
      }
   }

   private int traceDown() {
      int retval = 0;
      int y = (int)Math.round(mc.player.getY()) - 1;

      for (int tracey = y; tracey >= 0; tracey--) {
         HitResult trace = mc.level
            .clip(new ClipContext(mc.player.position(), new Vec3(mc.player.getX(), tracey, mc.player.getZ()), Block.COLLIDER, Fluid.NONE, mc.player));
         if (trace != null && trace.getType() == Type.BLOCK) {
            return retval;
         }

         retval++;
      }

      return retval;
   }

   private boolean trace() {
      AABB bbox = mc.player.getBoundingBox();
      Vec3 basepos = bbox.getCenter();
      double minX = bbox.minX;
      double minZ = bbox.minZ;
      double maxX = bbox.maxX;
      double maxZ = bbox.maxZ;
      Map<Vec3, Vec3> positions = new HashMap<>();
      positions.put(basepos, new Vec3(basepos.x, basepos.y - 1.0, basepos.z));
      positions.put(new Vec3(minX, basepos.y, minZ), new Vec3(minX, basepos.y - 1.0, minZ));
      positions.put(new Vec3(maxX, basepos.y, minZ), new Vec3(maxX, basepos.y - 1.0, minZ));
      positions.put(new Vec3(minX, basepos.y, maxZ), new Vec3(minX, basepos.y - 1.0, maxZ));
      positions.put(new Vec3(maxX, basepos.y, maxZ), new Vec3(maxX, basepos.y - 1.0, maxZ));

      for (Vec3 key : positions.keySet()) {
         ClipContext context = new ClipContext(key, positions.get(key), Block.COLLIDER, Fluid.NONE, mc.player);
         BlockHitResult result = mc.level.clip(context);
         if (result != null && result.getType() == Type.BLOCK) {
            return false;
         }
      }

      BlockState state = mc.level.getBlockState(new BlockPosX(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ()));
      return state.isAir();
   }

   private static enum Mode {
      Fast,
      None;
   }
}
