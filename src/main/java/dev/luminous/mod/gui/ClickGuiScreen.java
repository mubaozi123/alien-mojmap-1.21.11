package dev.luminous.mod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.Mod;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.BindButton;
import dev.luminous.mod.gui.items.buttons.ModuleButton;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class ClickGuiScreen extends Screen {
   private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
   private final ArrayList<Component> components = new ArrayList<>();

   public ClickGuiScreen() {
      super(net.minecraft.network.chat.Component.literal("Alien"));
      this.setInstance();
      this.load();
   }

   public static ClickGuiScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGuiScreen();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   private void load() {
      int x = -84;

      for (final Module.Category category : Module.Category.values()) {
         ArrayList var10000 = this.components;
         String var10004 = category.toString();
         x += 94;
         var10000.add(new Component(var10004, category, x, 4, true) {
            @Override
            public void setupItems() {
               for (Module module : Alien.MODULE.getModules()) {
                  if (module.getCategory().equals(category)) {
                     this.addButton(new ModuleButton(module));
                  }
               }
            }
         });
      }

      this.components.forEach(components -> components.getItems().sort(Comparator.comparing(Mod::getName)));
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      float keyCodec = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * keyCodec;
      float slideY = (1.0F - keyCodec) * 20.0F;
      Item.context = context;
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (Component c : this.components) {
         minX = Math.min(minX, c.getX());
         minY = Math.min(minY, c.getY());
         maxX = Math.max(maxX, c.getX() + c.getWidth());
         maxY = Math.max(maxY, c.getY() + c.getHeight());
      }

      int margin = 16;
      int panelX = Math.max(8, minX - margin);
      int panelY = Math.max(6, minY - margin);
      int panelW = Math.min(context.guiWidth() - panelX - 8, maxX - minX + margin * 2);
      int panelH = Math.min(context.guiHeight() - panelY - 6, maxY - minY + margin * 2 + 24);
      context.pose().pushMatrix();
      context.pose().translate(panelX + panelW / 2.0F, panelY + panelH / 2.0F + slideY);
      context.pose().scale(scale, scale);
      context.pose().translate(-(panelX + panelW / 2.0F), -(panelY + panelH / 2.0F));
      context.pose().popMatrix();
      context.pose().pushMatrix();
      context.pose().translate(0.0F, slideY);
      context.pose().scale(scale, scale);
      this.components.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
      context.pose().popMatrix();
   }

   private int toContentX(double mouseX, float scale) {
      return Math.round((float)mouseX / scale);
   }

   private int toContentY(double mouseY, float scale, float slideY) {
      return Math.round(((float)mouseY - slideY) / scale);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      float alpha = (float)ClickGui.getInstance().alphaValue;
      float scale = 0.92F + 0.08F * alpha;
      float slideY = (1.0F - alpha) * 20.0F;
      int x = this.toContentX(click.x(), scale);
      int y = this.toContentY(click.y(), scale, slideY);
      this.components.forEach(components -> components.mouseClicked(x, y, click.button()));
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      this.components.forEach(components -> components.mouseReleased((int)click.x(), (int)click.y(), click.button()));
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InputConstants.isKeyDown(Wrapper.mc.getWindow(), 340)) {
         if (verticalAmount < 0.0) {
            this.components.forEach(component -> component.setX(component.getX() - 15));
         } else if (verticalAmount > 0.0) {
            this.components.forEach(component -> component.setX(component.getX() + 15));
         }
      } else if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 15));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 15));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return !BindButton.ANY_LISTENING;
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      boolean bindListening = BindButton.ANY_LISTENING;
      if (input.isEscape() && bindListening) {
         int key = input.key();
         this.components.forEach(component -> component.onKeyPressed(key));
         return true;
      } else {
         this.components.forEach(component -> component.onKeyPressed(input.key()));
         return super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharacterEvent input) {
      this.components.forEach(component -> component.onKeyTyped((char)input.codepoint(), input.modifiers()));
      return super.charTyped(input);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   public final ArrayList<Component> getComponents() {
      return this.components;
   }

   public int getTextOffset() {
      return -ClickGui.getInstance().textOffset.getValueInt() - 6;
   }
}
