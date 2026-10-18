package dev.luminous.mod.modules.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ClickType;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   final Queue<ServerboundContainerClickPacket> storedClicks = new LinkedList<>();
   final AtomicBoolean pause = new AtomicBoolean();
   private final EnumSetting<NoSlow.Mode> mode = this.add(new EnumSetting<>("Mode", NoSlow.Mode.Vanilla));
   private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", true));
   private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
   private final BooleanSetting climb = this.add(new BooleanSetting("Climb", false));
   private final BooleanSetting gui = this.add(new BooleanSetting("Gui", true));
   private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false, this.gui::getValue));
   private final EnumSetting<NoSlow.Bypass> clickBypass = this.add(new EnumSetting<>("GuiMoveBypass", NoSlow.Bypass.None));
   boolean using = false;
   int delay = 0;

   public NoSlow() {
      super("NoSlow", Module.Category.Movement);
      this.setChinese("无减速");
      INSTANCE = this;
   }

   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.using = mc.player.isUsingItem();
      this.delay--;
      if (this.using) {
         this.delay = 2;
      }

      if (this.using && !mc.player.isHandsBusy() && !mc.player.isFallFlying()) {
         switch ((NoSlow.Mode)this.mode.getValue()) {
            case NCP:
               mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
               break;
            case Grim:
               if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               }
               break;
            case GrimPacket:
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 1, 0, ClickType.PICKUP, mc.player);
               if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               }
         }
      }

      if (this.gui.getValue() && !(mc.screen instanceof ChatScreen)) {
         for (KeyMapping k : new KeyMapping[]{mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight}) {
            k.setDown(InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(k.saveString()).getValue()));
         }

         mc.options
            .keyJump
            .setDown(
               ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue()
                  || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())
            );
         mc.options
            .keyUp
            .setDown(AutoWalk.INSTANCE.forward() || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyUp.saveString()).getValue()));
         mc.options
            .keySprint
            .setDown(
               Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()
                  || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keySprint.saveString()).getValue())
            );
         if (this.allowSneak.getValue()) {
            mc.options.keyShift.setDown(InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyShift.saveString()).getValue()));
         }
      }
   }

   @EventListener(priority = 100)
   public void keyboard(KeyboardInputEvent event) {
      if (this.sneak.getValue()) {
         event.cancel();
      }

      if (this.gui.getValue() && !(mc.screen instanceof ChatScreen)) {
         for (KeyMapping k : new KeyMapping[]{mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight}) {
            k.setDown(InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(k.saveString()).getValue()));
         }

         mc.options
            .keyJump
            .setDown(
               ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue()
                  || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())
            );
         mc.options
            .keyUp
            .setDown(AutoWalk.INSTANCE.forward() || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyUp.saveString()).getValue()));
         mc.options
            .keySprint
            .setDown(
               Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()
                  || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keySprint.saveString()).getValue())
            );
         if (this.allowSneak.getValue()) {
            mc.options.keyShift.setDown(InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyShift.saveString()).getValue()));
         }

         boolean pf = mc.options.keyUp.isDown();
         boolean pb = mc.options.keyDown.isDown();
         boolean pl = mc.options.keyLeft.isDown();
         boolean pr = mc.options.keyRight.isDown();
         mc.player.input.keyPresses = new Input(
            pf && !pb, pb && !pf, pl && !pr, pr && !pl, mc.options.keyJump.isDown(), mc.options.keyShift.isDown(), mc.options.keySprint.isDown()
         );
      }
   }

   @EventListener
   public void onUse(InteractItemEvent event) {
      if (event.isPre()) {
         if (this.delay > 0) {
            mc.rightClickDelay = 0;
            event.cancel();
         } else if (this.mode.is(NoSlow.Mode.GrimPacket)
            && mc.player != null
            && mc.player.getItemInHand(event.hand).getItem().components().has(DataComponents.FOOD)) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 1, 0, ClickType.PICKUP, mc.player);
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send e) {
      if (!nullCheck()) {
         ServerboundUseItemPacket packet;
         Packet<?> packet2;
         if (this.mode.is(NoSlow.Mode.Drop)
            && (packet2 = e.getPacket()) instanceof ServerboundUseItemPacket
            && (packet = (ServerboundUseItemPacket)packet2).getHand() == InteractionHand.MAIN_HAND
            && mc.player.getMainHandItem().getItem().components().has(DataComponents.FOOD)) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
         } else if (MovementUtil.isMoving() && !this.pause.get()) {
            if (e.getPacket() instanceof ServerboundContainerClickPacket click) {
               switch ((NoSlow.Bypass)this.clickBypass.getValue()) {
                  case NCP:
                     if (mc.player.onGround()
                        && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getConnection()
                              .send(
                                 new ServerboundPlayerCommandPacket(
                                    mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
                                 )
                              );
                        }

                        mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false, false));
                     }
                     break;
                  case NCP2:
                     if (mc.player.onGround()
                        && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, 2.71875E-7, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getConnection()
                              .send(
                                 new ServerboundPlayerCommandPacket(
                                    mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
                                 )
                              );
                        }

                        mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false, false));
                     }
                     break;
                  case Grim:
                     if (click.clickType() != ClickType.PICKUP && click.clickType() != ClickType.PICKUP_ALL) {
                        mc.getConnection().send(new ServerboundContainerClosePacket(0));
                     }
                     break;
                  case Delay:
                     this.storedClicks.add(click);
                     e.cancel();
               }
            }

            if (e.getPacket() instanceof ServerboundContainerClosePacket && this.clickBypass.is(NoSlow.Bypass.Delay)) {
               this.pause.set(true);

               while (!this.storedClicks.isEmpty()) {
                  mc.getConnection().send(this.storedClicks.poll());
               }

               this.pause.set(false);
            }
         }
      }
   }

   @EventListener
   public void onPacketSendPost(PacketEvent.Sent e) {
      if (e.getPacket() instanceof ServerboundContainerClickPacket && mc.player.isSprinting() && this.clickBypass.is(NoSlow.Bypass.NCP)) {
         mc.getConnection()
            .send(new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING));
      }
   }

   public boolean noSlow() {
      return this.isOn()
         && this.mode.getValue() != NoSlow.Mode.None
         && (this.mode.getValue() != NoSlow.Mode.Drop && this.mode.getValue() != NoSlow.Mode.GrimPacket || this.using);
   }

   public boolean soulSand() {
      return this.isOn() && this.soulSand.getValue();
   }

   public boolean climb() {
      return this.isOn() && this.climb.getValue();
   }

   private static enum Bypass {
      None,
      NCP,
      NCP2,
      Grim,
      Delay;
   }

   public static enum Mode {
      Vanilla,
      NCP,
      Grim,
      GrimPacket,
      Drop,
      None;
   }
}
