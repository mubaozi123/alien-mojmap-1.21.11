package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.util.Objects;
import net.minecraft.world.effect.MobEffects;

public class Strafe extends Module {
   public static Strafe INSTANCE;
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", true));
   private final BooleanSetting slowCheck = this.add(new BooleanSetting("SlowCheck", true));

   public Strafe() {
      super("Strafe", "Modifies sprinting", Module.Category.Movement);
      this.setChinese("灵活移动");
      INSTANCE = this;
   }

   @EventListener
   public void onStrafe(MoveEvent event) {
      if (!BaritoneUtil.isActive()) {
         if (!mc.player.isShiftKeyDown()
            && !Fly.INSTANCE.isOn()
            && !HoleSnap.INSTANCE.isOn()
            && !Speed.INSTANCE.isOn()
            && !mc.player.isFallFlying()
            && !EntityUtil.isInsideBlock()
            && !mc.player.isInLava()
            && !mc.player.isInWater()
            && !mc.player.getAbilities().flying) {
            if (!MovementUtil.isMoving()) {
               if (this.airStop.getValue()) {
                  MovementUtil.setMotionX(0.0);
                  MovementUtil.setMotionZ(0.0);
               }
            } else {
               double[] dir = MovementUtil.directionSpeed(this.getBaseMoveSpeed());
               event.setX(dir[0]);
               event.setZ(dir[1]);
            }
         }
      }
   }

   public double getBaseMoveSpeed() {
      double n = 0.2873;
      if (mc.player.hasEffect(MobEffects.SPEED) && (!this.slowCheck.getValue() || !mc.player.hasEffect(MobEffects.SLOWNESS))) {
         n *= 1.0 + 0.2 * (Objects.requireNonNull(mc.player.getEffect(MobEffects.SPEED)).getAmplifier() + 1);
      }

      return n;
   }
}
