package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.BlockHitResult;

public class PacketLogger extends Module {
   public static PacketLogger INSTANCE;
   private final BooleanSetting moveFull = this.add(new BooleanSetting("MoveFull", true));
   private final BooleanSetting movePos = this.add(new BooleanSetting("MovePosition", true));
   private final BooleanSetting moveLook = this.add(new BooleanSetting("MoveLook", true));
   private final BooleanSetting moveGround = this.add(new BooleanSetting("MoveGround", true));
   private final BooleanSetting vehicleMove = this.add(new BooleanSetting("VehicleMove", true));
   private final BooleanSetting playerAction = this.add(new BooleanSetting("PlayerAction", true));
   private final BooleanSetting updateSlot = this.add(new BooleanSetting("UpdateSlot", true));
   private final BooleanSetting handSwing = this.add(new BooleanSetting("HandSwing", true));
   private final BooleanSetting pong = this.add(new BooleanSetting("Pong", true));
   private final BooleanSetting interactEntity = this.add(new BooleanSetting("InteractEntity", true));
   private final BooleanSetting interactBlock = this.add(new BooleanSetting("InteractBlock", true));
   private final BooleanSetting interactItem = this.add(new BooleanSetting("InteractItem", true));
   private final BooleanSetting closeScreen = this.add(new BooleanSetting("CloseScreen", true));
   private final BooleanSetting command = this.add(new BooleanSetting("ClientCommand", true));
   private final BooleanSetting status = this.add(new BooleanSetting("ClientStatus", true));
   private final BooleanSetting clickSlot = this.add(new BooleanSetting("ClickSlot", true));
   private final BooleanSetting pickInventory = this.add(new BooleanSetting("PickInventory", true));
   private final BooleanSetting teleportConfirm = this.add(new BooleanSetting("TeleportConfirm", true));
   private final BooleanSetting s2cVelocity = this.add(new BooleanSetting("S2cVelocity", true));

   public PacketLogger() {
      super("PacketLogger", Module.Category.Misc);
      this.setChinese("数据包记录");
      INSTANCE = this;
   }

   private void logPacket(String msg, Object... args) {
      String s = String.format(msg, args);
      CommandManager.sendMessage(s);
   }

   @EventListener(priority = 999999)
   public void velocity(EntityVelocityUpdateEvent event) {
      if (this.s2cVelocity.getValue() && event.getEntity() == mc.player) {
         this.logPacket("S2C Velocity, x: %s, y: %s, z: %s, isExplosion: %s", event.getX(), event.getY(), event.getZ(), event.isExplosion());
      }
   }

   @EventListener(priority = -999999)
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof PosRot packet && this.moveFull.getValue()) {
         StringBuilder builder = new StringBuilder();
         builder.append("PlayerMove Full - ");
         if (packet.hasPosition()) {
            builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         if (packet.hasRotation()) {
            builder.append("yaw: ").append(packet.getYRot(0.0F)).append(", pitch: ").append(packet.getXRot(0.0F)).append(" ");
         }

         builder.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builder.toString());
      }

      if (event.getPacket() instanceof Pos packet && this.movePos.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove PosGround - ");
         if (packet.hasPosition()) {
            builderx.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof Rot packet && this.moveLook.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove LookGround - ");
         if (packet.hasRotation()) {
            builderx.append("yaw: ").append(packet.getYRot(0.0F)).append(", pitch: ").append(packet.getXRot(0.0F)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof StatusOnly packet && this.moveGround.getValue()) {
         String s = "PlayerMove Ground - onground: " + packet.isOnGround();
         this.logPacket(s);
      }

      if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet && this.vehicleMove.getValue()) {
         this.logPacket(
            "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s",
            packet.position().x(),
            packet.position().y(),
            packet.position().z(),
            packet.yRot(),
            packet.xRot()
         );
      }

      if (event.getPacket() instanceof ServerboundPlayerActionPacket packet && this.playerAction.getValue() && packet.getDirection() != null) {
         this.logPacket(
            "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString()
         );
      }

      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet && this.updateSlot.getValue()) {
         this.logPacket("UpdateSlot - slot: %d", packet.getSlot());
      }

      if (event.getPacket() instanceof ServerboundSwingPacket packet && this.handSwing.getValue()) {
         this.logPacket("HandSwing - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof ServerboundPongPacket packet && this.pong.getValue()) {
         this.logPacket("Pong - %d", packet.getId());
      }

      if (event.getPacket() instanceof ServerboundInteractPacket packet && this.interactEntity.getValue()) {
         this.logPacket("InteractEntity - Entity: %s, id: %s", Criticals.getEntity(packet).getName().getString(), Criticals.getEntity(packet).getId());
      }

      if (event.getPacket() instanceof ServerboundUseItemOnPacket packet && this.interactBlock.getValue()) {
         BlockHitResult blockHitResult = packet.getHitResult();
         this.logPacket(
            "InteractBlock - pos: %s, dir: %s, hand: %s",
            blockHitResult.getBlockPos().toShortString(),
            blockHitResult.getDirection().name(),
            packet.getHand().name()
         );
      }

      if (event.getPacket() instanceof ServerboundUseItemPacket packet && this.interactItem.getValue()) {
         this.logPacket("InteractItem - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof ServerboundContainerClosePacket packet && this.closeScreen.getValue()) {
         this.logPacket("CloseScreen - id: %s", packet.getContainerId());
      }

      if (event.getPacket() instanceof ServerboundPlayerCommandPacket packet && this.command.getValue()) {
         this.logPacket("ClientCommand - mode: %s", packet.getAction().name());
      }

      if (event.getPacket() instanceof ServerboundClientCommandPacket packet && this.status.getValue()) {
         this.logPacket("ClientStatus - mode: %s", packet.getAction().name());
      }

      if (event.getPacket() instanceof ServerboundContainerClickPacket packet && this.clickSlot.getValue()) {
         this.logPacket(
            "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.clickType().name(), packet.slotNum(), packet.buttonNum(), packet.containerId()
         );
      }

      if (event.getPacket() instanceof ServerboundContainerClickPacket packet
         && packet.clickType() == ClickType.CLONE
         && packet.buttonNum() == 2
         && this.pickInventory.getValue()) {
         this.logPacket("PickInventory - slot: %s", packet.slotNum());
      }

      if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket packet && this.teleportConfirm.getValue()) {
         this.logPacket("TeleportConfirm - id: %s", packet.getId());
      }
   }
}
