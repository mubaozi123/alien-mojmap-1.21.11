package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.Module;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PopManager implements Wrapper {
   public final HashMap<String, Integer> popContainer = new HashMap<>();
   private final List<Player> deadPlayer = new ArrayList<>();

   public PopManager() {
      this.init();
   }

   public void init() {
      Alien.EVENT_BUS.subscribe(this);
   }

   public int getPop(String s) {
      return this.popContainer.getOrDefault(s, 0);
   }

   public int getPop(Player player) {
      return this.getPop(player.getName().getString());
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         for (AbstractClientPlayer player : Alien.THREAD.getPlayers()) {
            if (player == null || !player.isDeadOrDying()) {
               this.deadPlayer.remove(player);
            } else if (!this.deadPlayer.contains(player)) {
               Alien.EVENT_BUS.post(DeathEvent.get(player));
               this.onDeath(player);
               this.deadPlayer.add(player);
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         Packet<?> packet2 = event.getPacket();
         Entity entity;
         ClientboundEntityEventPacket packet;
         if (packet2 instanceof ClientboundEntityEventPacket
            && (packet = (ClientboundEntityEventPacket)packet2).getEventId() == 35
            && (entity = packet.getEntity(mc.level)) instanceof Player) {
            Player player = (Player)entity;
            this.onTotemPop(player);
         }
      }
   }

   public void onDeath(Player player) {
      this.popContainer.remove(player.getName().getString());
   }

   public void onTotemPop(Player player) {
      int l_Count = 1;
      if (this.popContainer.containsKey(player.getName().getString())) {
         l_Count = this.popContainer.get(player.getName().getString());
         this.popContainer.put(player.getName().getString(), ++l_Count);
      } else {
         this.popContainer.put(player.getName().getString(), l_Count);
      }

      Alien.EVENT_BUS.post(TotemEvent.get(player));
   }
}
