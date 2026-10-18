package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.level.block.Blocks;

public class BedCrafter extends Module {
   public static BedCrafter INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5, 0, 8));
   private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));

   public BedCrafter() {
      super("BedCrafter", Module.Category.Misc);
      this.setChinese("自动制作床");
      INSTANCE = this;
   }

   public static int getEmptySlots() {
      int emptySlots = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.isEmpty()) {
            emptySlots++;
         }
      }

      return emptySlots;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      int wool = 0;
      int plank = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() == Items.WHITE_WOOL
            || stack.getItem() == Items.LIGHT_GRAY_WOOL
            || stack.getItem() == Items.GRAY_WOOL
            || stack.getItem() == Items.BLACK_WOOL
            || stack.getItem() == Items.BROWN_WOOL
            || stack.getItem() == Items.RED_WOOL
            || stack.getItem() == Items.ORANGE_WOOL
            || stack.getItem() == Items.YELLOW_WOOL
            || stack.getItem() == Items.LIME_WOOL
            || stack.getItem() == Items.GREEN_WOOL
            || stack.getItem() == Items.CYAN_WOOL
            || stack.getItem() == Items.LIGHT_BLUE_WOOL
            || stack.getItem() == Items.BLUE_WOOL
            || stack.getItem() == Items.PURPLE_WOOL
            || stack.getItem() == Items.MAGENTA_WOOL
            || stack.getItem() == Items.PINK_WOOL) {
            wool += stack.getCount();
         }

         if (stack.getItem() == Items.OAK_PLANKS
            || stack.getItem() == Items.SPRUCE_PLANKS
            || stack.getItem() == Items.BIRCH_PLANKS
            || stack.getItem() == Items.JUNGLE_PLANKS
            || stack.getItem() == Items.ACACIA_PLANKS
            || stack.getItem() == Items.DARK_OAK_PLANKS
            || stack.getItem() == Items.MANGROVE_PLANKS
            || stack.getItem() == Items.CHERRY_PLANKS
            || stack.getItem() == Items.BAMBOO_PLANKS) {
            plank += stack.getCount();
         }
      }

      if (wool >= 3 && plank >= 3) {
         if (getEmptySlots() == 0) {
            if (mc.player.containerMenu instanceof CraftingMenu) {
               mc.player.closeContainer();
            }

            if (this.disable.getValue()) {
               this.disable();
            }
         } else {
            if (mc.player.containerMenu instanceof CraftingMenu craftingHandler) {
               int maxPerTick = 2;
               int crafted = 0;

               for (RecipeCollection recipeResult : mc.player.getRecipeBook().getCollections()) {
                  for (RecipeDisplayEntry recipeDisplayEntry : recipeResult.getRecipes()) {
                     RecipeDisplay display = recipeDisplayEntry.display();
                     if (display.result() instanceof ItemStackSlotDisplay stackSlotDisplay) {
                        ItemStack previewStack = stackSlotDisplay.stack();
                        if (previewStack.getItem() instanceof BedItem) {
                           while (crafted < maxPerTick && getEmptySlots() > 0) {
                              mc.gameMode.handlePlaceRecipe(craftingHandler.containerId, recipeDisplayEntry.id(), false);
                              mc.gameMode.handleInventoryMouseClick(craftingHandler.containerId, 0, 1, ClickType.QUICK_MOVE, mc.player);
                              crafted++;
                           }

                           if (crafted >= maxPerTick) {
                              return;
                           }
                        }
                     }
                  }
               }

               mc.player.closeContainer();
               if (this.disable.getValue()) {
                  this.disable();
               }
            } else {
               this.doPlace();
            }
         }
      }
   }

   private void doPlace() {
      BlockPos bestPos = null;
      double distance = 100.0;
      boolean foundTable = false;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat())) {
         if (mc.level.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
            bestPos = pos;
            foundTable = true;
            break;
         }

         if (BlockUtil.canPlace(pos)) {
            double dist = mc.player.distanceToSqr(pos.getCenter());
            if (bestPos == null || dist < distance) {
               bestPos = pos;
               distance = dist;
            }
         }
      }

      if (bestPos != null) {
         if (foundTable) {
            BlockUtil.clickBlock(bestPos, BlockUtil.getClickSide(bestPos), this.rotate.getValue());
         } else {
            if (InventoryUtil.findItem(Items.CRAFTING_TABLE) == -1) {
               return;
            }

            int old = mc.player.getInventory().selected;
            InventoryUtil.switchToSlot(InventoryUtil.findItem(Items.CRAFTING_TABLE));
            BlockUtil.placeBlock(bestPos, this.rotate.getValue());
            InventoryUtil.switchToSlot(old);
         }
      }
   }
}
