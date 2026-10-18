package dev.luminous.api.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class HeldItemRendererEvent {
   private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();
   private InteractionHand hand;
   private ItemStack item;
   private float ep;
   private PoseStack stack;

   private HeldItemRendererEvent() {
   }

   public static HeldItemRendererEvent get(InteractionHand hand, ItemStack item, float equipProgress, PoseStack stack) {
      INSTANCE.hand = hand;
      INSTANCE.item = item;
      INSTANCE.ep = equipProgress;
      INSTANCE.stack = stack;
      return INSTANCE;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public float getEp() {
      return this.ep;
   }

   public PoseStack getStack() {
      return this.stack;
   }
}
