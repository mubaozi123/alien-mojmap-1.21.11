package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoLog extends Module {
   public static boolean loggedOut = false;
   private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
   private final BooleanSetting onPop = this.add(new BooleanSetting("OnPop", true));
   private final BooleanSetting lowArmor = this.add(new BooleanSetting("LowArmor", true));
   private final BooleanSetting totemLess = this.add(new BooleanSetting("TotemLess", true).setParent());
   private final SliderSetting totems = this.add(new SliderSetting("Totems", 2.0, 0.0, 20.0, 1.0, this.totemLess::isOpen));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final BooleanSetting showReason = this.add(new BooleanSetting("ShowReason", false));

   public AutoLog() {
      super("AutoLog", Module.Category.Misc);
      this.setChinese("自动下线");
   }

   @Override
   public void onEnable() {
      if (this.logOnEnable.getValue()) {
         this.disconnect("Enabled");
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      int totem;
      if (this.totemLess.getValue() && (totem = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING)) <= this.totems.getValue()) {
         this.disconnect("You have too few totems (" + totem + ").");
      } else {
         if (this.lowArmor.getValue()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
               if (slot.isArmor()) {
                  ItemStack armor = mc.player.getItemBySlot(slot);
                  if (!armor.isEmpty() && EntityUtil.getDamagePercent(armor) < 5) {
                     this.disconnect("Your armor has keyCodec durability of less than 5%.");
                     return;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPop(TotemEvent event) {
      if (this.onPop.getValue() && event.getPlayer() == mc.player) {
         this.disconnect("You poped 1 totem!");
      }
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }
   }

   private void disconnect(String reason) {
      loggedOut = true;
      CommandManager.sendMessage("§4[AutoLog] " + reason);
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(114514));
      if (this.showReason.getValue()) {
         mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal("[AutoLog]" + reason)));
      }
   }
}
