package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PlaySoundEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundSystem {
   @Inject(
      method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onPlay(SoundInstance soundInstance, CallbackInfoReturnable<PlayResult> info) {
      PlaySoundEvent event = PlaySoundEvent.get(soundInstance);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.setReturnValue(PlayResult.NOT_STARTED);
      }
   }
}
