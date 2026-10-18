package dev.luminous.mod.gui.windows;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.modules.Module;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class WindowsScreen extends Screen {
   public static WindowBase lastClickedWindow;
   public static WindowBase draggingWindow;
   private List<WindowBase> windows = new ArrayList<>();

   public WindowsScreen(WindowBase... windows) {
      super(Component.nullToEmpty("CustomWindows"));
      this.windows.clear();
      lastClickedWindow = null;
      draggingWindow = null;
      this.windows = Arrays.stream(windows).toList();
   }

   private List<WindowBase> getVisibleWindows() {
      List<WindowBase> ordered = new ArrayList<>(this.windows.size());

      for (WindowBase window : this.windows) {
         if (window != lastClickedWindow && window.isVisible()) {
            ordered.add(window);
         }
      }

      if (lastClickedWindow != null && lastClickedWindow.isVisible()) {
         ordered.add(lastClickedWindow);
      }

      return ordered;
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (Module.nullCheck()) {
         this.renderBackground(context, mouseX, mouseY, delta);
      }

      for (WindowBase window : this.getVisibleWindows()) {
         window.render(context, mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      double mouseX = click.x();
      double mouseY = click.y();
      List<WindowBase> ordered = this.getVisibleWindows();

      for (int i = ordered.size() - 1; i >= 0; i--) {
         WindowBase window = ordered.get(i);
         if (Render2DUtil.isHovered(mouseX, mouseY, window.getX(), window.getY(), window.getWidth() + 10.0F, window.getHeight())) {
            lastClickedWindow = window;
            window.mouseClicked(mouseX, mouseY, click.button());
            return true;
         }
      }

      int ix = Wrapper.mc.getWindow().getGuiScaledWidth() / 2;
      float offset = this.windows.size() * 20.0F / -2.0F - 23.0F;
      if (Render2DUtil.isHovered(mouseX, mouseY, ix + offset + 1.0F, Wrapper.mc.getWindow().getGuiScaledHeight() - 23, 15.0, 15.0)) {
         Wrapper.mc.setScreen(ClickGuiScreen.getInstance());
         return true;
      } else {
         offset += 23.0F;

         for (WindowBase window : this.windows) {
            if (Render2DUtil.isHovered(mouseX, mouseY, ix + offset, Wrapper.mc.getWindow().getGuiScaledHeight() - 24, 17.0, 17.0)) {
               window.setVisible(!window.isVisible());
            }

            offset += 20.0F;
         }

         return true;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      for (WindowBase window : this.windows) {
         window.mouseReleased(click.x(), click.y(), click.button());
      }

      return true;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
      for (WindowBase window : this.getVisibleWindows()) {
         window.mouseDragged(click.x(), click.y(), click.button());
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (WindowBase window : this.getVisibleWindows()) {
         window.mouseScrolled((int)(verticalAmount * 5.0));
      }

      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      List<WindowBase> ordered = this.getVisibleWindows();

      for (int i = ordered.size() - 1; i >= 0; i--) {
         if (ordered.get(i).keyPressed(input.key(), input.scancode(), input.modifiers())) {
            return true;
         }
      }

      return super.keyPressed(input);
   }

   @Override
   public boolean charTyped(CharacterEvent input) {
      List<WindowBase> ordered = this.getVisibleWindows();

      for (int i = ordered.size() - 1; i >= 0; i--) {
         if (ordered.get(i).charTyped((char)input.codepoint(), 0)) {
            return true;
         }
      }

      return super.charTyped(input);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
