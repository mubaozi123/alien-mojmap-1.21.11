package dev.luminous.mod.modules.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.mod.modules.Module;
import net.minecraft.world.entity.player.Input;

public class MovementSync extends Module {
   public static MovementSync INSTANCE;

   public MovementSync() {
      super("MovementSync", Module.Category.Movement);
      this.setChinese("移动同步");
      INSTANCE = this;
   }

   @EventListener(priority = 100)
   public void keyboard(KeyboardInputEvent event) {
      mc.options
         .keyJump
         .setDown(
            ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue()
               || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())
         );
      mc.options
         .keyUp
         .setDown(AutoWalk.INSTANCE.forward() || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyUp.saveString()).getValue()));
      boolean pf = mc.options.keyUp.isDown();
      boolean pb = mc.options.keyDown.isDown();
      boolean pl = mc.options.keyLeft.isDown();
      boolean pr = mc.options.keyRight.isDown();
      mc.player.input.keyPresses = new Input(
         pf && !pb, pb && !pf, pl && !pr, pr && !pl, mc.options.keyJump.isDown(), mc.options.keyShift.isDown(), mc.options.keySprint.isDown()
      );
   }
}
