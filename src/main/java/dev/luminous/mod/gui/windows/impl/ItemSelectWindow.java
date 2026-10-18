package dev.luminous.mod.gui.windows.impl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.ImmediateDrawUtil;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.CleanerManager;
import dev.luminous.core.impl.FontManager;
import dev.luminous.core.impl.TradeManager;
import dev.luminous.core.impl.XrayManager;
import dev.luminous.mod.gui.items.buttons.StringButton;
import dev.luminous.mod.gui.windows.WindowBase;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemSelectWindow extends WindowBase {
   private final Manager manager;
   private final ArrayList<ItemSelectWindow.ItemPlate> itemPlates = new ArrayList<>();
   private final ArrayList<ItemSelectWindow.ItemPlate> allItems = new ArrayList<>();
   private boolean allTab = true;
   private boolean listening = false;
   private String search = "Search";

   public ItemSelectWindow(Manager manager) {
      this(Wrapper.mc.getWindow().getGuiScaledWidth() / 2.0F - 100.0F, Wrapper.mc.getWindow().getGuiScaledHeight() / 2.0F - 150.0F, 200.0F, 300.0F, manager);
   }

   public ItemSelectWindow(float x, float y, float width, float height, Manager manager) {
      super(x, y, width, height, "Items", null);
      this.manager = manager;
      this.refreshItemPlates();
      int id1 = 0;

      for (Block block : BuiltInRegistries.BLOCK) {
         this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), block.asItem(), block.getDescriptionId()));
         id1++;
      }

      for (Item item : BuiltInRegistries.ITEM) {
         this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), item, item.getDescriptionId()));
         id1++;
      }
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY) {
      super.render(context, mouseX, mouseY);
      boolean hover1 = Render2DUtil.isHovered(mouseX, mouseY, this.getX() + this.getWidth() - 90.0F, this.getY() + 3.0F, 70.0, 10.0);
      Render2DUtil.drawRect(
         context.pose(),
         this.getX() + this.getWidth() - 90.0F,
         this.getY() + 3.0F,
         70.0F,
         10.0F,
         hover1 ? new Color(-981236861, true) : new Color(-984131753, true)
      );
      FontManager.small.drawString(context.pose(), this.search, this.getX() + this.getWidth() - 86.0F, this.getY() + 7.0F, new Color(14013909).getRGB());
      int tabColor1 = this.allTab ? new Color(14013909).getRGB() : Color.GRAY.getRGB();
      int tabColor2 = this.allTab ? Color.GRAY.getRGB() : new Color(12434877).getRGB();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(this.getX() + 1.5F, this.getY() + 29.0F, 0.0F).setColor(Color.DARK_GRAY.getRGB());
      bufferBuilder.addVertex(this.getX() + 8.0F, this.getY() + 29.0F, 0.0F).setColor(tabColor1);
      bufferBuilder.addVertex(this.getX() + 8.0F, this.getY() + 19.0F, 0.0F).setColor(tabColor1);
      bufferBuilder.addVertex(this.getX() + 48.0F, this.getY() + 19.0F, 0.0F).setColor(tabColor1);
      bufferBuilder.addVertex(this.getX() + 54.0F, this.getY() + 29.0F, 0.0F).setColor(tabColor1);
      bufferBuilder.addVertex(this.getX() + 52.0F, this.getY() + 25.0F, 0.0F).setColor(tabColor2);
      bufferBuilder.addVertex(this.getX() + 52.0F, this.getY() + 19.0F, 0.0F).setColor(tabColor2);
      bufferBuilder.addVertex(this.getX() + 92.0F, this.getY() + 19.0F, 0.0F).setColor(tabColor2);
      bufferBuilder.addVertex(this.getX() + 100.0F, this.getY() + 29.0F, 0.0F).setColor(Color.GRAY.getRGB());
      bufferBuilder.addVertex(this.getX() + this.getWidth() - 1.0F, this.getY() + 29.0F, 0.0F).setColor(Color.DARK_GRAY.getRGB());
      ImmediateDrawUtil.drawGui(bufferBuilder);
      FontManager.small.drawString(context.pose(), "All", this.getX() + 25.0F, this.getY() + 25.0F, tabColor1);
      FontManager.small.drawString(context.pose(), "Selected", this.getX() + 60.0F, this.getY() + 25.0F, tabColor2);
      if (!this.allTab && this.itemPlates.isEmpty()) {
         FontManager.ui
            .drawCenteredString(
               context.pose(), "It's empty here yet", this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, new Color(12434877).getRGB()
            );
      }

      context.enableScissor((int)this.getX(), (int)(this.getY() + 30.0F), (int)(this.getX() + this.getWidth()), (int)(this.getY() + this.getHeight() - 1.0F));

      for (ItemSelectWindow.ItemPlate itemPlate : this.allTab ? this.allItems : this.itemPlates) {
         if (!(itemPlate.offset + this.getY() + 25.0F + this.getScrollOffset() > this.getY() + this.getHeight())
            && !(itemPlate.offset + this.getScrollOffset() + this.getY() + 10.0F < this.getY())) {
            context.renderItem(
               itemPlate.item().getDefaultInstance(), (int)this.getX() + 6, (int)(itemPlate.offset + this.getY() + 32.0F + this.getScrollOffset())
            );
            FontManager.ui
               .drawString(
                  context.pose(),
                  I18n.get(itemPlate.key()),
                  this.getX() + 26.0F,
                  itemPlate.offset + this.getY() + 38.0F + this.getScrollOffset(),
                  new Color(12434877).getRGB()
               );
            boolean hover2 = Render2DUtil.isHovered(
               mouseX, mouseY, this.getX() + this.getWidth() - 20.0F, itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset(), 11.0, 11.0
            );
            Render2DUtil.drawRect(
               context.pose(),
               this.getX() + this.getWidth() - 20.0F,
               itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset(),
               11.0F,
               11.0F,
               hover2 ? new Color(-981828998, true) : new Color(-984131753, true)
            );
            boolean selected = this.itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key, itemPlate.key));
            if (this.allTab && !selected) {
               FontManager.ui
                  .drawString(context.pose(), "+", this.getX() + this.getWidth() - 17.0F, itemPlate.offset + this.getY() + 37.0F + this.getScrollOffset(), -1);
            } else {
               FontManager.ui
                  .drawString(context.pose(), "-", this.getX() + this.getWidth() - 16.5, itemPlate.offset + this.getY() + 37.5 + this.getScrollOffset(), -1);
            }
         }
      }

      this.setMaxElementsHeight((this.allTab ? this.allItems : this.itemPlates).size() * 20);
      context.disableScissor();
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + this.getWidth() - 90.0F, this.getY() + 3.0F, 70.0, 10.0)) {
         this.listening = true;
         this.search = "";
         this.refreshAllItems();
      } else {
         super.mouseClicked(mouseX, mouseY, button);
         if (this.isVisible()) {
            if (this.listening) {
               this.listening = false;
               if (this.search.isEmpty()) {
                  this.search = "Search";
               }

               this.refreshAllItems();
            }

            if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + 8.0F, this.getY() + 19.0F, 44.0, 19.0)) {
               this.allTab = true;
               this.resetScroll();
            }

            if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + 54.0F, this.getY() + 19.0F, 46.0, 19.0)) {
               this.allTab = false;
               this.resetScroll();
            }

            for (ItemSelectWindow.ItemPlate itemPlate : Lists.newArrayList(this.allTab ? this.allItems : this.itemPlates)) {
               float rowY = itemPlate.offset + this.getY() + 35.0F + this.getScrollOffset();
               if (!(rowY < this.getY() + 30.0F) && !(rowY + 10.0F > this.getY() + this.getHeight())) {
                  String name = itemPlate.key().replace("item.minecraft.", "").replace("block.minecraft.", "");
                  if (Render2DUtil.isHovered(mouseX, mouseY, this.getX() + this.getWidth() - 20.0F, rowY, 11.0, 11.0)) {
                     boolean selected = this.itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key(), itemPlate.key));
                     if (this.allTab && !selected) {
                        if (this.manager instanceof TradeManager m) {
                           if (!m.inWhitelist(name)) {
                              m.add(name);
                              this.refreshItemPlates();
                           }
                        } else if (this.manager instanceof CleanerManager mx) {
                           if (!mx.inList(name)) {
                              mx.add(name);
                              this.refreshItemPlates();
                           }
                        } else if (this.manager instanceof XrayManager mxx && !mxx.inWhitelist(name)) {
                           mxx.add(name);
                           this.refreshItemPlates();
                        }
                     } else {
                        if (this.manager instanceof TradeManager mxx) {
                           mxx.remove(name);
                        } else if (this.manager instanceof CleanerManager mxx) {
                           mxx.remove(name);
                        } else if (this.manager instanceof XrayManager mxx) {
                           mxx.remove(name);
                        }

                        this.refreshItemPlates();
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean altDown = (modifiers & 6) != 0
         || InputConstants.isKeyDown(Wrapper.mc.getWindow(), 342)
         || InputConstants.isKeyDown(Wrapper.mc.getWindow(), 346)
         || InputConstants.isKeyDown(Wrapper.mc.getWindow(), 341)
         || InputConstants.isKeyDown(Wrapper.mc.getWindow(), 345);
      if (keyCode == 70 && altDown) {
         this.listening = !this.listening;
         if (this.listening) {
            this.search = "";
            this.refreshAllItems();
         }

         return true;
      } else if (!this.listening) {
         return false;
      } else {
         switch (keyCode) {
            case 32:
               if (this.search.equals("Search")) {
                  this.search = "";
               }

               this.search = this.search + " ";
               this.refreshAllItems();
               break;
            case 256:
            case 257:
            case 335:
               this.listening = false;
               if (this.search.isEmpty()) {
                  this.search = "Search";
               }

               this.refreshAllItems();
               break;
            case 259:
               this.search = StringButton.removeLastChar(this.search);
               this.refreshAllItems();
               if (this.search.isEmpty()) {
                  this.listening = false;
                  this.search = "Search";
               }
         }

         return true;
      }
   }

   @Override
   public boolean charTyped(char key, int keyCode) {
      if (this.listening && StringUtil.isAllowedChatCharacter(key)) {
         if (this.search.equals("Search")) {
            this.search = "";
         }

         this.search = this.search + key;
         this.refreshAllItems();
         return true;
      } else {
         return false;
      }
   }

   private void refreshItemPlates() {
      this.itemPlates.clear();
      int id = 0;

      for (Item item : BuiltInRegistries.ITEM) {
         if (this.manager instanceof TradeManager m) {
            if (m.inWhitelist(item.getDescriptionId())) {
               this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getDescriptionId()));
               id++;
            }
         } else if (this.manager instanceof CleanerManager mx) {
            if (mx.inList(item.getDescriptionId())) {
               this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getDescriptionId()));
               id++;
            }
         } else if (this.manager instanceof XrayManager mxx && mxx.inWhitelist(item.getDescriptionId())) {
            this.itemPlates.add(new ItemSelectWindow.ItemPlate((float)id, (float)(id * 20), item.asItem(), item.getDescriptionId()));
            id++;
         }
      }
   }

   private void refreshAllItems() {
      this.allItems.clear();
      this.resetScroll();
      int id1 = 0;

      for (Item item : BuiltInRegistries.ITEM) {
         if (this.search.equals("Search")
            || this.search.isEmpty()
            || item.getDescriptionId().contains(this.search)
            || item.getName().getString().toLowerCase().contains(this.search.toLowerCase())) {
            this.allItems.add(new ItemSelectWindow.ItemPlate((float)id1, (float)(id1 * 20), item, item.getDescriptionId()));
            id1++;
         }
      }
   }

   private record ItemPlate(float id, float offset, Item item, String key) {
   }
}
