package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;

public class InteractTweaks extends Module {
   public static InteractTweaks INSTANCE;
   public final BooleanSetting noEntityTrace = this.add(new BooleanSetting("NoEntityTrace", true).setParent());
   public final BooleanSetting onlyPickaxe = this.add(new BooleanSetting("OnlyPickaxe", true, this.noEntityTrace::isOpen));
   public final BooleanSetting multiTask = this.add(new BooleanSetting("MultiTask", true));
   public final BooleanSetting respawn = this.add(new BooleanSetting("Respawn", true));
   public final BooleanSetting ghostHand = this.add(new BooleanSetting("IgnoreBedrock", false));
   private final BooleanSetting noAbort = this.add(new BooleanSetting("NoMineAbort", false));
   private final BooleanSetting noReset = this.add(new BooleanSetting("NoMineReset", false));
   private final BooleanSetting noDelay = this.add(new BooleanSetting("NoMineDelay", false));
   private final BooleanSetting noInteract = this.add(new BooleanSetting("NoInteract", false));
   private final BooleanSetting pickaxeSwitch = this.add(new BooleanSetting("SwitchEat", false).setParent());
   private final BooleanSetting allowSword = this.add(new BooleanSetting("Sword", true, this.pickaxeSwitch::isOpen));
   private final BooleanSetting allowPickaxe = this.add(new BooleanSetting("Pickaxe", true, this.pickaxeSwitch::isOpen));
   private final BooleanSetting reach = this.add(new BooleanSetting("Reach", false));
   public final SliderSetting blockRange = this.add(new SliderSetting("BlockRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   public final SliderSetting entityRange = this.add(new SliderSetting("EntityRange", 5.0, 0.0, 15.0, 0.1, this.reach::getValue));
   private final SliderSetting delay = this.add(new SliderSetting("UseDelay", 4.0, 0.0, 4.0, 1.0));
   public boolean isActive;
   boolean swapped = false;
   int lastSlot = 0;

   public InteractTweaks() {
      super("InteractTweaks", Module.Category.Player);
      this.setChinese("交互调整");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.respawn.getValue() && mc.screen instanceof DeathScreen) {
         mc.player.respawn();
         mc.setScreen(null);
      }

      if (mc.rightClickDelay <= 4 - this.delay.getValueInt()) {
         mc.rightClickDelay = 0;
      }

      if (this.pickaxeSwitch.getValue()) {
         if ((!mc.player.getMainHandItem().is(ItemTags.PICKAXES) || !this.allowPickaxe.getValue())
            && (!mc.player.getMainHandItem().is(ItemTags.SWORDS) || !this.allowSword.getValue())
            && mc.player.getMainHandItem().getItem() != Items.ENCHANTED_GOLDEN_APPLE
            && mc.player.getMainHandItem().getItem() != Items.GOLDEN_APPLE) {
            this.swapped = false;
            return;
         }

         int gapple = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
         if (gapple == -1) {
            gapple = InventoryUtil.findItem(Items.GOLDEN_APPLE);
         }

         if (gapple == -1) {
            if (this.swapped) {
               InventoryUtil.switchToSlot(this.lastSlot);
               this.swapped = false;
            }

            return;
         }

         if (mc.options.keyUse.isDown()) {
            if ((
                  mc.player.getMainHandItem().is(ItemTags.PICKAXES) && this.allowPickaxe.getValue()
                     || mc.player.getMainHandItem().is(ItemTags.SWORDS) && this.allowSword.getValue()
               )
               && mc.player.getOffhandItem().getItem() != Items.ENCHANTED_GOLDEN_APPLE
               && mc.player.getMainHandItem().getItem() != Items.GOLDEN_APPLE) {
               this.lastSlot = mc.player.getInventory().selected;
               InventoryUtil.switchToSlot(gapple);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.lastSlot);
            this.swapped = false;
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      Packet<?> packet;
      if (!nullCheck() && this.noInteract.getValue() && (packet = event.getPacket()) instanceof ServerboundUseItemOnPacket) {
         ServerboundUseItemOnPacket packet2 = (ServerboundUseItemOnPacket)packet;
         Block block = mc.level.getBlockState(packet2.getHitResult().getBlockPos()).getBlock();
         if (!mc.player.isShiftKeyDown() && (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof AnvilBlock)) {
            event.cancel();
         }
      }
   }

   @Override
   public void onDisable() {
      this.isActive = false;
   }

   public boolean reach() {
      return this.isOn() && this.reach.getValue();
   }

   public boolean noAbort() {
      return this.isOn() && this.noAbort.getValue() && !mc.options.keyUse.isDown();
   }

   public boolean noReset() {
      return this.isOn() && this.noReset.getValue();
   }

   public boolean noDelay() {
      return this.isOn() && this.noDelay.getValue();
   }

   public boolean multiTask() {
      return this.isOn() && this.multiTask.getValue();
   }

   public boolean noEntityTrace() {
      if (this.isOff() || !this.noEntityTrace.getValue()) {
         return false;
      } else {
         return !this.onlyPickaxe.getValue()
            ? true
            : mc.player.getMainHandItem().is(ItemTags.PICKAXES) || mc.player.isUsingItem() && !mc.player.getMainHandItem().is(ItemTags.SWORDS);
      }
   }

   public boolean ghostHand() {
      return this.isOn() && this.ghostHand.getValue() && !mc.options.keyUse.isDown() && !mc.options.keyShift.isDown();
   }
}
