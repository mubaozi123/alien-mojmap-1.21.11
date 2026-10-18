package dev.luminous.api.events.impl;

import net.minecraft.client.gui.GuiGraphics;

public class Render2DEvent {
   private static final Render2DEvent INSTANCE = new Render2DEvent();
   public GuiGraphics drawContext;
   public float tickDelta;

   public static Render2DEvent get(GuiGraphics drawContext, float tickDelta) {
      INSTANCE.drawContext = drawContext;
      INSTANCE.tickDelta = tickDelta;
      return INSTANCE;
   }
}
