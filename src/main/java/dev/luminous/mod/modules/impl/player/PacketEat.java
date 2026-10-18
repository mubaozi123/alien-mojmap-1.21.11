package dev.luminous.mod.modules.impl.player;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;

public class PacketEat extends Module {
   public static PacketEat INSTANCE;
   private final BooleanSetting deSync = this.add(new BooleanSetting("DeSync", false));
   private final BooleanSetting noRelease = this.add(new BooleanSetting("NoRelease", true));

   public PacketEat() {
      super("PacketEat", Module.Category.Player);
      this.setChinese("发包进食");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.deSync.getValue() && mc.player.isUsingItem() && mc.player.getUseItem().getItem().components().has(DataComponents.FOOD)) {
         Module.sendSequencedPacket(
            id -> new ServerboundUseItemPacket(mc.player.getUsedItemHand(), id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
         );
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (this.noRelease.getValue()
         && event.getPacket() instanceof ServerboundPlayerActionPacket packet
         && packet.getAction() == Action.RELEASE_USE_ITEM
         && mc.player.getUseItem().getItem().components().has(DataComponents.FOOD)) {
         event.cancel();
      }
   }
}
