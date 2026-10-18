package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.GameLeftEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.Fonts;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.misc.AutoLog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType;

public class ServerManager implements Wrapper {
   public final Timer playerNull = new Timer();
   public int currentSlot = -1;
   private final ArrayDeque<Float> tpsResult = new ArrayDeque<>(20);
   boolean worldNull = true;
   private long time;
   private long tickTime;
   private float tps;
   int lastSlot;

   public ServerManager() {
      Alien.EVENT_BUS.subscribe(this);
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

   @EventListener(priority = -200)
   public void onPacket(PacketEvent.Send event) {
      if (AntiCheat.INSTANCE.attackCDFix.getValue()) {
         if (event.isCancelled()) {
            return;
         }

         Packet<?> packet = event.getPacket();
         if (!(packet instanceof ServerboundSwingPacket)
            && (!(packet instanceof ServerboundInteractPacket) || Criticals.getInteractType((ServerboundInteractPacket)packet) != ActionType.ATTACK)) {
            if (packet instanceof ServerboundSetCarriedItemPacket packet2 && this.lastSlot != packet2.getSlot()) {
               this.lastSlot = packet2.getSlot();
               mc.player.resetOnlyAttackStrengthTicker();
            }
         } else {
            mc.player.resetOnlyAttackStrengthTicker();
         }
      }
   }

   @EventListener
   public void onLeft(GameLeftEvent event) {
      this.currentSlot = -1;
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
         int packetSlot = packet.getSlot();
         if (AntiCheat.INSTANCE.noBadSlot.getValue() && packetSlot == this.currentSlot) {
            event.cancel();
            return;
         }

         this.currentSlot = packetSlot;
      }
   }

   public float getTPS() {
      return round2(this.tps);
   }

   public float getCurrentTPS() {
      return round2(20.0F * ((float)this.tickTime / 1000.0F));
   }

   public float getTPSFactor() {
      return this.getTPS() / 20.0F;
   }

   @EventListener(priority = 999)
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket) {
         if (this.time != 0L) {
            this.tickTime = System.currentTimeMillis() - this.time;
            if (this.tpsResult.size() > 20) {
               this.tpsResult.poll();
            }

            this.tpsResult.add(20.0F * (1000.0F / (float)this.tickTime));
            float average = 0.0F;

            for (Float value : this.tpsResult) {
               average += MathUtil.clamp(value, 0.0F, 20.0F);
            }

            this.tps = average / this.tpsResult.size();
         }

         this.time = System.currentTimeMillis();
      }
   }

   public void onUpdate() {
      if (mc.player == null) {
         this.playerNull.reset();
      }

      if (this.worldNull && mc.level != null) {
         Fonts.INSTANCE.refresh();
         AutoLog.loggedOut = false;
         Alien.MODULE.onLogin();
         this.worldNull = false;
      } else if (!this.worldNull && mc.level == null) {
         Alien.save();
         Alien.MODULE.onLogout();
         this.worldNull = true;
      }
   }
}
