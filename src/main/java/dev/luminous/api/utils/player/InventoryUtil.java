package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class InventoryUtil implements Wrapper {
   static int lastSlot = -1;
   static int lastSelect = -1;

   public static void inventorySwap(int slot, int selectedSlot) {
      if (slot == lastSlot) {
         switchToSlot(lastSelect);
         lastSlot = -1;
         lastSelect = -1;
      } else if (slot - 36 != selectedSlot) {
         if (EntityUtil.inInventory()) {
            if (AntiCheat.INSTANCE.invSwapBypass.getValue()) {
               if (slot - 36 >= 0) {
                  lastSlot = slot;
                  lastSelect = selectedSlot;
                  switchToSlot(slot - 36);
                  return;
               }

               mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            } else {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, selectedSlot, ClickType.SWAP, mc.player);
               mc.player.getInventory().tick();
            }
         }
      }
   }

   public static void switchToSlot(int slot) {
      mc.player.getInventory().selected = slot;
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
   }

   public static int findItem(Item input) {
      for (int i = 0; i < 9; i++) {
         Item item = mc.player.getInventory().getItem(i).getItem();
         if (Item.getId(item) == Item.getId(input)) {
            return i;
         }
      }

      return -1;
   }

   public static int getFood() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).getComponents().has(DataComponents.FOOD)) {
            return i;
         }
      }

      return -1;
   }

   public static int getPotionCount(MobEffect targetEffect) {
      int count = 0;

      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (Item.getId(itemStack.getItem()) == Item.getId(Items.SPLASH_POTION)) {
            PotionContents potionContentsComponent = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            for (MobEffectInstance effect : potionContentsComponent.getAllEffects()) {
               if (effect.getEffect().value() == targetEffect) {
                  count += itemStack.getCount();
               }
            }
         }
      }

      return count;
   }

   public static int getItemCount(Class<?> clazz) {
      int count = 0;

      for (Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)entry.getValue().getItem()).getBlock())) {
            count += entry.getValue().getCount();
         }
      }

      return count;
   }

   public static int getItemCount(Item item) {
      int count = 0;

      for (Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
         if (entry.getValue().getItem() == item) {
            count += entry.getValue().getCount();
         }
      }

      if (mc.player.getOffhandItem().getItem() == item) {
         count += mc.player.getOffhandItem().getCount();
      }

      return count;
   }

   public static int findClass(Class<?> clazz) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != ItemStack.EMPTY) {
            if (clazz.isInstance(stack.getItem())) {
               return i;
            }

            if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
               return i;
            }
         }
      }

      return -1;
   }

   public static int findClassInventorySlot(Class<?> clazz) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return i < 9 ? i + 36 : i;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      } else {
         for (int ix = 35; ix >= 0; ix--) {
            ItemStack stack = mc.player.getInventory().getItem(ix);
            if (stack != ItemStack.EMPTY) {
               if (clazz.isInstance(stack.getItem())) {
                  return ix < 9 ? ix + 36 : ix;
               }

               if (stack.getItem() instanceof BlockItem && clazz.isInstance(((BlockItem)stack.getItem()).getBlock())) {
                  return ix < 9 ? ix + 36 : ix;
               }
            }
         }
      }

      return -1;
   }

   public static int findSword() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.is(ItemTags.SWORDS)) {
            return i;
         }
      }

      return -1;
   }

   public static int findSwordInventorySlot() {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(ItemTags.SWORDS)) {
               return i < 9 ? i + 36 : i;
            }
         }
      } else {
         for (int ix = 35; ix >= 0; ix--) {
            ItemStack stack = mc.player.getInventory().getItem(ix);
            if (stack.is(ItemTags.SWORDS)) {
               return ix < 9 ? ix + 36 : ix;
            }
         }
      }

      return -1;
   }

   public static int findBlock(Block blockIn) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() == blockIn) {
            return i;
         }
      }

      return -1;
   }

   public static int findUnBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlock() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem
            && !BlockUtil.isClickable(Block.byItem(stack.getItem()))
            && ((BlockItem)stack.getItem()).getBlock() != Blocks.COBWEB) {
            return i;
         }
      }

      return -1;
   }

   public static int findBlockInventorySlot(Block block) {
      return findItemInventorySlot(block.asItem());
   }

   public static int findItemInventorySlot(Item item) {
      if (AntiCheat.INSTANCE.priorHotbar.getValue()) {
         return findItemInventorySlotFromZero(item.asItem());
      } else {
         for (int i = 35; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == item) {
               return i < 9 ? i + 36 : i;
            }
         }

         return -1;
      }
   }

   public static int findItemInventorySlotFromZero(Item item) {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
      HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<>();

      for (int current = 0; current <= 35; current++) {
         fullInventorySlots.put(current, mc.player.getInventory().getItem(current));
      }

      return fullInventorySlots;
   }
}
