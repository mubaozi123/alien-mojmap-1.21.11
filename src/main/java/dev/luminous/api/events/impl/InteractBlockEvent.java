package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.world.InteractionHand;

public class InteractBlockEvent extends Event {
   private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
   public InteractionHand hand;

   private InteractBlockEvent() {
   }

   public static InteractBlockEvent getPre(InteractionHand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Pre;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public static InteractBlockEvent getPost(InteractionHand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Post;
      return INSTANCE;
   }
}
