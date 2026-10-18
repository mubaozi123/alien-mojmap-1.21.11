package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.SprintEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.TickMovementEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;

public class Sprint extends Module {
   public static Sprint INSTANCE;
   public final EnumSetting<Sprint.Mode> mode = this.add(new EnumSetting<>("Mode", Sprint.Mode.Legit));
   public final BooleanSetting inWaterPause = this.add(new BooleanSetting("InWaterPause", true));
   public final BooleanSetting inWebPause = this.add(new BooleanSetting("InWebPause", true));
   public final BooleanSetting sneakingPause = this.add(new BooleanSetting("SneakingPause", false));
   public final BooleanSetting blindnessPause = this.add(new BooleanSetting("BlindnessPause", false));
   public final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
   public final BooleanSetting lagPause = this.add(new BooleanSetting("LagPause", true));
   boolean pause = false;

   public Sprint() {
      super("Sprint", "Permanently keeps player in sprinting mode.", Module.Category.Movement);
      this.setChinese("强制疾跑");
      INSTANCE = this;
   }

   public static float getSprintYaw(float yaw) {
      if (mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
         if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
            yaw -= 45.0F;
         } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
            yaw += 45.0F;
         }
      } else if (mc.options.keyDown.isDown() && !mc.options.keyUp.isDown()) {
         yaw += 180.0F;
         if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
            yaw += 45.0F;
         } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
            yaw -= 45.0F;
         }
      } else if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
         yaw -= 90.0F;
      } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
         yaw += 90.0F;
      }

      return Mth.wrapDegrees(yaw);
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (this.lagPause.getValue() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.pause = true;
      }
   }

   public boolean inWater() {
      return this.inWaterPause.getValue() && mc.player.isInLiquid();
   }

   @EventListener
   public void onMove(TickMovementEvent event) {
      if (!BaritoneUtil.isPathing()) {
         if (!this.inWater()) {
            if (this.mode.getValue() == Sprint.Mode.PressKey) {
               mc.options.keySprint.setDown(true);
            } else {
               mc.player.setSprinting(this.shouldSprint());
            }
         }
      }
   }

   @EventListener
   public void tick(TickEvent event) {
      if (event.isPost()) {
         this.pause = false;
      }
   }

   @EventListener
   public void sprint(SprintEvent event) {
      if (!BaritoneUtil.isPathing() && !this.mode.is(Sprint.Mode.PressKey)) {
         if (!this.inWater()) {
            event.cancel();
            event.setSprint(this.shouldSprint());
         }
      }
   }

   private boolean shouldSprint() {
      if ((mc.player.getFoodData().getFoodLevel() > 6 || mc.player.isCreative())
         && MovementUtil.isMoving()
         && !this.pause
         && (!mc.player.isShiftKeyDown() || !this.sneakingPause.getValue())
         && (!Alien.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue())
         && (!mc.player.isUsingItem() || !this.usingPause.getValue())
         && !mc.player.isHandsBusy()
         && (!mc.player.hasEffect(MobEffects.BLINDNESS) || !this.blindnessPause.getValue())) {
         switch ((Sprint.Mode)this.mode.getValue()) {
            case Legit:
               return mc.player.input.hasForwardImpulse() && (!mc.player.horizontalCollision || mc.player.minorHorizontalCollision);
            case Rage:
               return true;
            case Rotation:
               if (AntiCheat.INSTANCE.movementSync()) {
                  return mc.player.zza > 0.0F;
               }

               return HoleSnap.INSTANCE.isOn() || Mth.degreesDifferenceAbs(getSprintYaw(mc.player.getYRot()), Alien.ROTATION.rotationYaw) < 40.0F;
         }
      }

      return false;
   }

   @EventListener(priority = -100)
   public void rotate(UpdateRotateEvent event) {
      if (!BaritoneUtil.isPathing()) {
         if ((
               (mc.player.getFoodData().getFoodLevel() > 6 || mc.player.isCreative())
                     && MovementUtil.isMoving()
                     && !Freecam.INSTANCE.isOn()
                     && !mc.player.isFallFlying()
                     && (!Alien.PLAYER.isInWeb(mc.player) || !this.inWebPause.getValue())
                     && (!mc.player.isShiftKeyDown() || !this.sneakingPause.getValue())
                     && !mc.player.isHandsBusy()
                     && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                     && !mc.player.isInLiquid()
                     && Freecam.INSTANCE.isOff()
                     && !mc.player.hasEffect(MobEffects.BLINDNESS)
                  || !this.blindnessPause.getValue()
            )
            && this.mode.is(Sprint.Mode.Rotation)
            && !event.isModified()) {
            event.setYaw(getSprintYaw(mc.player.getYRot()));
         }
      }
   }

   public static enum Mode {
      PressKey,
      Legit,
      Rage,
      Rotation;
   }
}
