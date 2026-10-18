package dev.luminous.api.events.impl;

import net.minecraft.world.entity.player.Player;

public class TotemEvent {
   private static final TotemEvent INSTANCE = new TotemEvent();
   private Player player;

   private TotemEvent() {
   }

   public static TotemEvent get(Player player) {
      INSTANCE.player = player;
      return INSTANCE;
   }

   public Player getPlayer() {
      return this.player;
   }
}
