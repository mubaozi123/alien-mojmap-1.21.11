package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class NoFall extends Module {
   private final EnumSetting<NoFall.NoFallMode> mode = this.add(new EnumSetting<>("Mode", NoFall.NoFallMode.Packet));
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 3.0, 0.0, 8.0, 0.1));

   public NoFall() {
      super("NoFall", "Prevents fall damage.", Module.Category.Player);
      this.setChinese("没有摔落伤害");
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(NoFall.NoFallMode.Grim) && this.checkFalling()) {
            mc.getConnection()
               .send(new PosRot(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), false, false));
            mc.player.resetFallDistance();
         }
      }
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > mc.player.getMaxFallDistance() && !mc.player.onGround() && !mc.player.isFallFlying();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() && mc.player.getItemBySlot(slot).getItem() == Items.ELYTRA) {
               return;
            }
         }

         if (this.mode.is(NoFall.NoFallMode.Packet)) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && mc.player.fallDistance >= (float)this.distance.getValue() && !BowBomb.send) {
               ((IPlayerMoveC2SPacket)packet).setOnGround(true);
            }
         }
      }
   }

   public static enum NoFallMode {
      Packet,
      Grim;
   }
}
