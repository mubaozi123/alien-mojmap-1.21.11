package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class AutoPot extends Module {
   public static AutoPot INSTANCE;
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting speed = this.add(new BooleanSetting("Speed", false));
   private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", false));
   private final BooleanSetting strength = this.add(new BooleanSetting("Strength", false));
   private final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
   private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));
   private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
   private final Timer delayTimer = new Timer();
   private boolean throwing = false;
   private boolean turtlePress;
   private boolean speedPress;
   private boolean strengthPress;

   public AutoPot() {
      super("AutoPot", Module.Category.Player);
      this.setChinese("自动药水");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new AutoPot.AutoPotTick());
   }

   public static int findPotionInventorySlot(MobEffect targetEffect) {
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (Item.getId(itemStack.getItem()) == Item.getId(Items.SPLASH_POTION)) {
            PotionContents potionContentsComponent = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            for (MobEffectInstance effect : potionContentsComponent.getAllEffects()) {
               if (effect.getEffect().value() == targetEffect) {
                  return i < 9 ? i + 36 : i;
               }
            }
         }
      }

      return -1;
   }

   public static int findPotion(MobEffect targetEffect) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (Item.getId(itemStack.getItem()) == Item.getId(Items.SPLASH_POTION)) {
            PotionContents potionContentsComponent = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            for (MobEffectInstance effect : potionContentsComponent.getAllEffects()) {
               if (effect.getEffect().value() == targetEffect) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   @Override
   public void onDisable() {
      this.throwing = false;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {
            if (!this.onlyGround.getValue()
               || (mc.player.onGround() || Alien.PLAYER.isInWeb(mc.player)) && !mc.level.isEmptyBlock(new BlockPosX(mc.player.position().add(0.0, -1.0, 0.0)))) {
               if (this.resistance.getValue() && (!mc.player.hasEffect(MobEffects.RESISTANCE) || mc.player.getEffect(MobEffects.RESISTANCE).getAmplifier() < 2)
                  )
                {
                  this.throwing = this.checkThrow(MobEffects.RESISTANCE.value());
                  if (this.isThrow()) {
                     this.throwPotion(MobEffects.RESISTANCE.value());
                     return;
                  }
               }

               if (this.speed.getValue() && !mc.player.hasEffect(MobEffects.SPEED)) {
                  this.throwing = this.checkThrow(MobEffects.SPEED.value());
                  if (this.isThrow()) {
                     this.throwPotion(MobEffects.SPEED.value());
                     return;
                  }
               }

               if (this.strength.getValue() && !mc.player.hasEffect(MobEffects.STRENGTH)) {
                  this.throwing = this.checkThrow(MobEffects.STRENGTH.value());
                  if (this.isThrow()) {
                     this.throwPotion(MobEffects.STRENGTH.value());
                     return;
                  }
               }

               if (this.slowFalling.getValue() && !mc.player.hasEffect(MobEffects.SLOW_FALLING)) {
                  this.throwing = this.checkThrow(MobEffects.SLOW_FALLING.value());
                  if (this.isThrow()) {
                     this.throwPotion(MobEffects.SLOW_FALLING.value());
                  }
               }
            }
         }
      }
   }

   public void throwPotion(MobEffect targetEffect) {
      int oldSlot = mc.player.getInventory().selected;
      int newSlot;
      if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selected);
         sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selected);
         EntityUtil.syncInventory();
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      } else if ((newSlot = findPotion(targetEffect)) != -1) {
         Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
         InventoryUtil.switchToSlot(newSlot);
         sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         InventoryUtil.switchToSlot(oldSlot);
         Alien.ROTATION.snapBack();
         this.delayTimer.reset();
      }
   }

   public boolean isThrow() {
      return this.throwing;
   }

   public boolean checkThrow(MobEffect targetEffect) {
      if (!EntityUtil.inInventory()) {
         return false;
      } else {
         return this.usingPause.getValue() && mc.player.isUsingItem()
            ? false
            : findPotion(targetEffect) != -1 || this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1;
      }
   }

   public class AutoPotTick {
      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!AutoPot.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.screen == null) {
                  if (AutoPot.this.resistanceKey.isPressed()) {
                     if (!AutoPot.this.turtlePress && AutoPot.this.checkThrow(MobEffects.RESISTANCE.value())) {
                        AutoPot.this.throwPotion(MobEffects.RESISTANCE.value());
                        AutoPot.this.turtlePress = true;
                        return;
                     }
                  } else {
                     AutoPot.this.turtlePress = false;
                  }

                  if (AutoPot.this.strengthKey.isPressed()) {
                     if (!AutoPot.this.strengthPress && AutoPot.this.checkThrow(MobEffects.STRENGTH.value())) {
                        AutoPot.this.throwPotion(MobEffects.STRENGTH.value());
                        AutoPot.this.strengthPress = true;
                        return;
                     }
                  } else {
                     AutoPot.this.strengthPress = false;
                  }

                  if (AutoPot.this.speedKey.isPressed()) {
                     if (!AutoPot.this.speedPress && AutoPot.this.checkThrow(MobEffects.SPEED.value())) {
                        AutoPot.this.throwPotion(MobEffects.SPEED.value());
                        AutoPot.this.speedPress = true;
                     }
                  } else {
                     AutoPot.this.speedPress = false;
                  }
               } else {
                  AutoPot.this.speedPress = false;
                  AutoPot.this.turtlePress = false;
                  AutoPot.this.strengthPress = false;
               }
            }
         }
      }
   }
}
