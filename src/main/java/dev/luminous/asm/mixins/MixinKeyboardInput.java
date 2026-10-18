package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.MovementInputEvent;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Inject(
      method = "tick",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/player/KeyboardInput;keyPresses:Lnet/minecraft/world/entity/player/Input;",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   private void keyInput(CallbackInfo ci) {
      KeyboardInputEvent event = KeyboardInputEvent.get();
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void keyInputTail(CallbackInfo ci) {
      Alien.EVENT_BUS.post(MovementInputEvent.get());
   }
}
