package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.world.InteractionHand;

public class InteractItemEvent extends Event {
   private static final InteractItemEvent INSTANCE = new InteractItemEvent();
   public InteractionHand hand;

   private InteractItemEvent() {
   }

   public static InteractItemEvent getPre(InteractionHand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Pre;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public static InteractItemEvent getPost(InteractionHand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Post;
      return INSTANCE;
   }
}
