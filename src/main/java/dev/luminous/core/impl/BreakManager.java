package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.BlockBreakingProgressEvent;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.ServerConnectBeginEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.BreakESP;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class BreakManager implements Wrapper {
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> breakMap = new ConcurrentHashMap<>();
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> doubleMap = new ConcurrentHashMap<>();

   public BreakManager() {
      Alien.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onServerConnectBegin(ServerConnectBeginEvent event) {
      this.breakMap.clear();
      this.doubleMap.clear();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!Module.nullCheck()) {
         if (AntiCheat.INSTANCE.detectDouble.getValue()) {
            for (int i : Alien.BREAK.doubleMap.keySet()) {
               BreakManager.BreakData breakData = Alien.BREAK.doubleMap.get(i);
               if (breakData == null
                  || breakData.getEntity() == null
                  || mc.level.isEmptyBlock(breakData.pos)
                  || breakData.timer
                     .passedMs(
                        Math.max(AntiCheat.INSTANCE.minTimeout.getValue() * 1000.0, breakData.breakTime * AntiCheat.INSTANCE.doubleMineTimeout.getValue())
                     )) {
                  Alien.BREAK.doubleMap.remove(i);
               }
            }
         }

         for (BreakManager.BreakData breakData : this.breakMap.values()) {
            breakData.breakTime = Math.max(BreakESP.getBreakTime(breakData.pos, false), 50.0);
            if (PacketMine.unbreakable(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = false;
               breakData.failed = true;
            } else if (mc.level.isEmptyBlock(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = true;
               breakData.failed = false;
            } else if (!breakData.complete && breakData.timer.passedMs(breakData.breakTime * AntiCheat.INSTANCE.breakTimeout.getValue())) {
               breakData.fade.setLength(0L);
               breakData.failed = true;
            } else {
               breakData.fade.setLength((long)breakData.breakTime);
            }
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet2) {
            if (packet2.getPos() == null) {
               return;
            }

            BreakManager.BreakData breakData = new BreakManager.BreakData(packet2.getPos(), packet2.getId(), false);
            if (breakData.getEntity() == null) {
               return;
            }

            if (Mth.sqrt((float)breakData.getEntity().getEyePosition().distanceToSqr(packet2.getPos().getCenter())) > 8.0F) {
               return;
            }

            if (AntiCheat.INSTANCE.detectDouble.getValue() && packet2.getProgress() != 255) {
               if (packet2.getProgress() != 0) {
                  BreakManager.BreakData doublePos = this.doubleMap.get(packet2.getId());
                  if (doublePos != null) {
                     doublePos.pos = packet2.getPos();
                     doublePos.timer.reset();
                  } else if (!PacketMine.unbreakable(packet2.getPos())) {
                     this.doubleMap.put(packet2.getId(), new BreakManager.BreakData(packet2.getPos(), packet2.getId(), true));
                  }

                  return;
               }

               BreakManager.BreakData doublePos = this.doubleMap.get(packet2.getId());
               if (doublePos != null && doublePos.pos.equals(packet2.getPos()) && !doublePos.timer.passedS(150.0)) {
                  return;
               }
            }

            BreakManager.BreakData current;
            if ((current = this.breakMap.get(packet2.getId())) != null && !current.failed && current.pos.equals(packet2.getPos())) {
               return;
            }

            this.breakMap.put(packet2.getId(), breakData);
            Alien.EVENT_BUS.post(BlockBreakingProgressEvent.get(packet2.getPos(), packet2.getId(), packet2.getProgress()));
            if (AntiCheat.INSTANCE.detectDouble.getValue() && !this.doubleMap.containsKey(packet2.getId()) && !PacketMine.unbreakable(packet2.getPos())) {
               this.doubleMap.put(packet2.getId(), new BreakManager.BreakData(packet2.getPos(), packet2.getId(), true));
            }
         }
      }
   }

   public boolean isMining(BlockPos pos) {
      return this.isMining(pos, true);
   }

   public boolean isMining(BlockPos pos, boolean self) {
      if (self && PacketMine.getBreakPos() != null && PacketMine.getBreakPos().equals(pos)) {
         return true;
      } else {
         for (BreakManager.BreakData breakData : this.breakMap.values()) {
            if (breakData.getEntity() != null
               && !(breakData.getEntity().getEyePosition().distanceTo(pos.getCenter()) > 7.0)
               && !breakData.failed
               && breakData.pos.equals(pos)) {
               return true;
            }
         }

         return false;
      }
   }

   public static class BreakData {
      public BlockPos pos;
      private final int entityId;
      public final FadeUtils fade;
      public final Timer timer;
      public double breakTime;
      public boolean failed = false;
      public boolean complete = false;

      public BreakData(BlockPos pos, int entityId, boolean extraBreak) {
         this.pos = pos;
         this.entityId = entityId;
         this.breakTime = Math.max(BreakESP.getBreakTime(pos, extraBreak), 50.0);
         this.fade = new FadeUtils((long)this.breakTime);
         this.timer = new Timer();
      }

      public Entity getEntity() {
         if (Wrapper.mc.level == null) {
            return null;
         } else {
            Entity entity = Wrapper.mc.level.getEntity(this.entityId);
            return entity instanceof Player ? entity : null;
         }
      }
   }
}
