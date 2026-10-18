package dev.luminous.api.events.impl;

import net.minecraft.world.entity.player.Player;

public class DeathEvent {
   private static final DeathEvent INSTANCE = new DeathEvent();
   private Player player;

   private DeathEvent() {
   }

   public static DeathEvent get(Player player) {
      INSTANCE.player = player;
      return INSTANCE;
   }

   public Player getPlayer() {
      return this.player;
   }
}
