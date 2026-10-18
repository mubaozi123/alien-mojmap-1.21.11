package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.world.entity.player.Player;

public class TravelEvent extends Event {
   private static final TravelEvent INSTANCE = new TravelEvent();
   private Player entity;

   private TravelEvent() {
   }

   public static TravelEvent get(Event.Stage stage, Player entity) {
      INSTANCE.entity = entity;
      INSTANCE.stage = stage;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Player getEntity() {
      return this.entity;
   }
}
