package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

public class AutoArmor extends Module {
   public static AutoArmor INSTANCE;
   private final EnumSetting<AutoArmor.EnchantPriority> head = this.add(new EnumSetting<>("Head", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> body = this.add(new EnumSetting<>("Body", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> tights = this.add(new EnumSetting<>("Tights", AutoArmor.EnchantPriority.Protection));
   private final EnumSetting<AutoArmor.EnchantPriority> feet = this.add(new EnumSetting<>("Feet", AutoArmor.EnchantPriority.Protection));
   private final BooleanSetting ignoreCurse = this.add(new BooleanSetting("IgnoreCurse", true));
   private final BooleanSetting noMove = this.add(new BooleanSetting("NoMove", false));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3.0, 0.0, 10.0, 1.0));
   private final BooleanSetting autoElytra = this.add(new BooleanSetting("AutoElytra", true));
   private final EnumSetting<AutoArmor.HotbarSwapMode> hotbarSwap = this.add(new EnumSetting<>("HotbarSwap", AutoArmor.HotbarSwapMode.Swap));
   private final EnumSetting<AutoArmor.InventorySwapMode> inventorySwap = this.add(new EnumSetting<>("InventorySwap", AutoArmor.InventorySwapMode.ClickSlot));
   private int tickDelay = 0;

   public AutoArmor() {
      super("AutoArmor", Module.Category.Player);
      this.setChinese("自动穿甲");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (EntityUtil.inInventory()) {
         if (mc.player.inventoryMenu == mc.player.containerMenu) {
            if (!MovementUtil.isMoving() || !this.noMove.getValue()) {
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  this.tickDelay = this.delay.getValueInt();
                  HashMap<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
                  armorMap.put(EquipmentSlot.FEET, new int[]{36, this.getProtection(mc.player.getInventory().getItem(36)), -1, -1});
                  armorMap.put(EquipmentSlot.LEGS, new int[]{37, this.getProtection(mc.player.getInventory().getItem(37)), -1, -1});
                  armorMap.put(EquipmentSlot.CHEST, new int[]{38, this.getProtection(mc.player.getInventory().getItem(38)), -1, -1});
                  armorMap.put(EquipmentSlot.HEAD, new int[]{39, this.getProtection(mc.player.getInventory().getItem(39)), -1, -1});

                  for (int s = 0; s < 36; s++) {
                     ItemStack stack = mc.player.getInventory().getItem(s);
                     if ((stack.get(DataComponents.EQUIPPABLE) != null || stack.is(Items.ELYTRA))
                        && (
                           !stack.is(Items.ELYTRA)
                              || (!ElytraFly.INSTANCE.isOff() || !this.autoElytra.getValue())
                                 && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.packet.getValue())
                        )) {
                        int protection = this.getProtection(stack);
                        EquipmentSlot slot = stack.is(Items.ELYTRA) ? EquipmentSlot.CHEST : stack.get(DataComponents.EQUIPPABLE).slot();

                        for (Entry e : armorMap.entrySet()) {
                           if (this.autoElytra.getValue() && ElytraFly.INSTANCE.isOn() && e.getKey() == EquipmentSlot.CHEST) {
                              if ((
                                    mc.player.getInventory().getItem(38).isEmpty()
                                       || !mc.player.getInventory().getItem(38).is(Items.ELYTRA)
                                       || !this.isElytraUsable(mc.player.getInventory().getItem(38))
                                 )
                                 && (
                                    ((int[])e.getValue())[2] == -1
                                       || mc.player.getInventory().getItem(((int[])e.getValue())[2]).isEmpty()
                                       || !mc.player.getInventory().getItem(((int[])e.getValue())[2]).is(Items.ELYTRA)
                                       || !this.isElytraUsable(mc.player.getInventory().getItem(((int[])e.getValue())[2]))
                                 )
                                 && !stack.isEmpty()
                                 && stack.is(Items.ELYTRA)
                                 && this.isElytraUsable(stack)) {
                                 ((int[])e.getValue())[2] = s;
                              }
                           } else if (protection > 0 && e.getKey() == slot && protection > ((int[])e.getValue())[1] && protection > ((int[])e.getValue())[3]) {
                              ((int[])e.getValue())[2] = s;
                              ((int[])e.getValue())[3] = protection;
                           }
                        }
                     }
                  }

                  for (Entry equipmentSlotEntry : armorMap.entrySet()) {
                     if (((int[])equipmentSlotEntry.getValue())[2] != -1) {
                        if (((int[])equipmentSlotEntry.getValue())[2] < 9) {
                           switch ((AutoArmor.HotbarSwapMode)this.hotbarSwap.getValue()) {
                              case Swap:
                                 int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                                 mc.gameMode
                                    .handleInventoryMouseClick(
                                       mc.player.containerMenu.containerId, armorSlot, ((int[])equipmentSlotEntry.getValue())[2], ClickType.SWAP, mc.player
                                    );
                                 EntityUtil.syncInventory();
                                 break;
                              case Switch:
                                 int old = mc.player.getInventory().selected;
                                 InventoryUtil.switchToSlot(((int[])equipmentSlotEntry.getValue())[2]);
                                 sendSequencedPacket(
                                    id -> new ServerboundUseItemPacket(
                                       InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()
                                    )
                                 );
                                 InventoryUtil.switchToSlot(old);
                           }
                        } else if (mc.player.inventoryMenu == mc.player.containerMenu) {
                           int armorSlot = 44 - ((int[])equipmentSlotEntry.getValue())[0];
                           int newArmorSlot = ((int[])equipmentSlotEntry.getValue())[2];
                           switch ((AutoArmor.InventorySwapMode)this.inventorySwap.getValue()) {
                              case ClickSlot:
                                 mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, newArmorSlot, 0, ClickType.PICKUP, mc.player);
                                 mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, armorSlot, 0, ClickType.PICKUP, mc.player);
                                 if (((int[])equipmentSlotEntry.getValue())[1] != -1) {
                                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, newArmorSlot, 0, ClickType.PICKUP, mc.player);
                                 }
                                 break;
                              case Pick:
                                 mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, newArmorSlot, 0, ClickType.PICKUP, mc.player);
                                 sendSequencedPacket(
                                    id -> new ServerboundUseItemPacket(
                                       InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()
                                    )
                                 );
                                 mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, newArmorSlot, 0, ClickType.PICKUP, mc.player);
                           }

                           EntityUtil.syncInventory();
                        }

                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private int getProtection(ItemStack is) {
      Equippable eq = is.get(DataComponents.EQUIPPABLE);
      if (eq == null && !is.is(Items.ELYTRA)) {
         return is.isEmpty() ? -1 : 0;
      } else {
         EquipmentSlot slot = is.is(Items.ELYTRA) ? EquipmentSlot.BODY : eq.slot();
         int prot = 0;
         if (is.is(Items.ELYTRA)) {
            if (!this.isElytraUsable(is)) {
               return 0;
            }

            prot = 1;
         } else {
            ItemAttributeModifiers mods = is.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (mods != null) {
               for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                  if (entry.attribute().value() == Attributes.ARMOR.value() && entry.modifier().operation() == Operation.ADD_VALUE) {
                     prot += (int)entry.modifier().amount();
                  }
               }
            }
         }

         int blastMultiplier = 1;
         int protectionMultiplier = 1;
         switch (slot) {
            case HEAD:
               if (this.head.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case BODY:
               if (this.body.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case LEGS:
               if (this.tights.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
               break;
            case FEET:
               if (this.feet.is(AutoArmor.EnchantPriority.Protection)) {
                  protectionMultiplier *= 2;
               } else {
                  blastMultiplier *= 2;
               }
         }

         if (is.isEnchanted()) {
            Registry<Enchantment> registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int protLevel = EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.PROTECTION), is);
            if (protLevel > 0) {
               prot += protLevel * protectionMultiplier;
            }

            int blastLevel = EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.BLAST_PROTECTION), is);
            if (blastLevel > 0) {
               prot += blastLevel * blastMultiplier;
            }

            if (this.ignoreCurse.getValue() && EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.BINDING_CURSE), is) > 0) {
               prot = -999;
            }
         }

         return prot;
      }
   }

   private boolean isElytraUsable(ItemStack stack) {
      return stack.getDamageValue() < stack.getMaxDamage() - 1;
   }

   private static enum EnchantPriority {
      Blast,
      Protection;
   }

   public static enum HotbarSwapMode {
      Swap,
      Switch;
   }

   public static enum InventorySwapMode {
      ClickSlot,
      Pick;
   }
}
