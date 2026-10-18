package dev.luminous.api.events.impl;

import net.minecraft.client.gui.GuiGraphics;

public class PreRender2DEvent {
   private static final PreRender2DEvent INSTANCE = new PreRender2DEvent();
   public GuiGraphics drawContext;

   public static PreRender2DEvent get(GuiGraphics drawContext) {
      INSTANCE.drawContext = drawContext;
      return INSTANCE;
   }
}
