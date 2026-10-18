package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.core.Holder.Reference;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class AutoMend extends Module {
   public static AutoMend INSTANCE;
   public final BooleanSetting rotation = this.add(new BooleanSetting("Rotation", true).setParent());
   private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, this.rotation::isOpen));
   public final BooleanSetting onlyBroken = this.add(new BooleanSetting("OnlyBroken", true));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 3, 0, 5));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   public final EnumSetting<SwingSide> interactSwing = this.add(new EnumSetting<>("InteractSwing", SwingSide.All));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true));
   public final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final Timer delayTimer = new Timer();
   boolean lookDown = false;
   int exp = 0;
   private boolean throwing = false;

   public AutoMend() {
      super("AutoMend", Module.Category.Combat);
      this.setChinese("自动经验瓶");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.throwing = this.checkThrow();
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.lookDown && this.isThrow() && this.delayTimer.passed(this.delay.getValueInt() * 20L) && (!this.onlyGround.getValue() || mc.player.onGround())
            )
          {
            this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE) - 1;
            if (this.rotation.getValue() && this.instant.getValue()) {
               Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 88.0F);
            }

            this.throwExp();
            if (this.rotation.getValue() && this.instant.getValue()) {
               Alien.ROTATION.snapBack();
            }
         }

         if (this.autoDisable.getValue() && !this.isThrow()) {
            this.disable();
         }
      }
   }

   @Override
   public void onEnable() {
      this.lookDown = !this.rotation.getValue() || this.instant.getValue();
      if (nullCheck()) {
         this.disable();
      } else {
         this.exp = InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE);
      }
   }

   @Override
   public String getInfo() {
      return String.valueOf(this.exp);
   }

   public void throwExp() {
      int oldSlot = mc.player.getInventory().selected;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE)) != -1) {
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selected);
         sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.interactSwing.getValue());
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selected);
         EntityUtil.syncInventory();
         this.delayTimer.reset();
      } else if ((newSlot = InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE)) != -1) {
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.interactSwing.getValue());
         InventoryUtil.switchToSlot(oldSlot);
         this.delayTimer.reset();
      }
   }

   @EventListener(priority = -200)
   public void RotateEvent(UpdateRotateEvent event) {
      if (this.rotation.getValue() && !this.instant.getValue()) {
         if (this.isThrow()) {
            event.setPitch(88.0F);
            this.lookDown = true;
         }
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow() {
      if (this.isOff()) {
         return false;
      } else if (mc.screen != null) {
         return false;
      } else if (this.usingPause.getValue() && mc.player.isUsingItem()) {
         return false;
      } else if (InventoryUtil.findItem(Items.EXPERIENCE_BOTTLE) != -1
         || this.inventory.getValue() && InventoryUtil.findItemInventorySlotFromZero(Items.EXPERIENCE_BOTTLE) != -1) {
         if (this.onlyBroken.getValue()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
               ItemStack armor = mc.player.getItemBySlot(slot);
               if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 100) {
                  ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(armor);
                  Reference<Enchantment> mending = mc.level.registryAccess().lookupOrThrow(Enchantments.MENDING.registryKey()).getOrThrow(Enchantments.MENDING);
                  return enchants.keySet().contains(mending);
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
