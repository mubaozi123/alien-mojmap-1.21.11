package dev.luminous.mod.gui;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class PeekScreen extends ShulkerBoxScreen {
   private final Identifier TEXTURE = Identifier.parse("textures/gui/container/shulker_box.png");
   private final ItemStack[] contents;
   private final ItemStack storageBlock;

   public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
      super(
         new ShulkerBoxMenu(0, Wrapper.mc.player.getInventory(), new SimpleContainer(contents)), Wrapper.mc.player.getInventory(), storageBlock.getHoverName()
      );
      this.contents = contents;
      this.storageBlock = storageBlock;
   }

   public static Color getShulkerColor(ItemStack shulkerItem) {
      if (shulkerItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock shulkerBlock) {
         DyeColor dye = shulkerBlock.getColor();
         if (dye == null) {
            return Color.WHITE;
         } else {
            int color = dye.getTextureDiffuseColor();
            return new Color(color);
         }
      } else {
         return Color.WHITE;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 2 && this.hoveredSlot != null && !this.hoveredSlot.getItem().isEmpty() && Wrapper.mc.player.containerMenu.getCarried().isEmpty()) {
         ItemStack itemStack = this.hoveredSlot.getItem();
         if (ShulkerViewer.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
            return ShulkerViewer.openContainer(this.hoveredSlot.getItem(), this.contents, false);
         }

         if (itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT) != null || itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT) != null) {
            this.onClose();
            Wrapper.mc.setScreen(new BookViewScreen(BookAccess.fromItem(itemStack)));
            return true;
         }
      }

      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !Wrapper.mc.options.keyInventory.matches(new KeyEvent(keyCode, scanCode, modifiers))) {
         return false;
      } else {
         this.onClose();
         return true;
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
      if (ClientSetting.INSTANCE.darkening.getValue()) {
         context.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, -2146430960);
      }

      Color color = getShulkerColor(this.storageBlock);
      int tint = ARGB.color(255, color.getRed(), color.getGreen(), color.getBlue());
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      context.blit(
         RenderPipelines.GUI_TEXTURED, this.TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight, 256, 256, tint
      );
   }
}
