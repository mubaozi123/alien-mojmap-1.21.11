package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public class AntiWeak extends Module {
   private final EnumSetting<AntiWeak.SwapMode> swapMode = this.add(new EnumSetting<>("SwapMode", AntiWeak.SwapMode.Inventory));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final BooleanSetting onlyCrystal = this.add(new BooleanSetting("OnlyCrystal", true));
   private final Timer delayTimer = new Timer();
   boolean ignore = false;
   private ServerboundInteractPacket lastPacket = null;

   public AntiWeak() {
      super("AntiWeak", Module.Category.Combat);
      this.setChinese("反虚弱");
   }

   @Override
   public String getInfo() {
      return this.swapMode.getValue().name();
   }

   @EventListener(priority = 200)
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (!event.isCancelled()) {
            if (!this.ignore) {
               if (mc.player.getEffect(MobEffects.WEAKNESS) != null) {
                  if (!mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
                     if (this.delayTimer.passedMs(this.delay.getValue())) {
                        Packet<?> packet2 = event.getPacket();
                        ServerboundInteractPacket packet;
                        if (packet2 instanceof ServerboundInteractPacket
                           && Criticals.getInteractType(packet = (ServerboundInteractPacket)packet2) == ActionType.ATTACK) {
                           if (this.onlyCrystal.getValue() && !(Criticals.getEntity(packet) instanceof EndCrystal)) {
                              return;
                           }

                           this.lastPacket = packet;
                           this.delayTimer.reset();
                           this.ignore = true;
                           this.doAnti();
                           this.ignore = false;
                           event.cancel();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doAnti() {
      if (this.lastPacket != null) {
         int strong = this.swapMode.getValue() != AntiWeak.SwapMode.Inventory ? InventoryUtil.findSword() : InventoryUtil.findSwordInventorySlot();
         if (strong != -1) {
            int old = mc.player.getInventory().selected;
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               InventoryUtil.switchToSlot(strong);
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selected);
            }

            mc.getConnection().send(this.lastPacket);
            if (this.swapMode.getValue() != AntiWeak.SwapMode.Inventory) {
               if (this.swapMode.getValue() != AntiWeak.SwapMode.Normal) {
                  InventoryUtil.switchToSlot(old);
               }
            } else {
               InventoryUtil.inventorySwap(strong, mc.player.getInventory().selected);
               EntityUtil.syncInventory();
            }
         }
      }
   }

   public static enum SwapMode {
      Normal,
      Silent,
      Inventory;
   }
}
