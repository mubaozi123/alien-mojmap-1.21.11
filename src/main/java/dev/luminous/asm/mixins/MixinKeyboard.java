package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard implements Wrapper {
   @Inject(method = "keyPress", at = @At("HEAD"))
   private void onKey(long windowPointer, int action, KeyEvent input, CallbackInfo ci) {
      try {
         int key = input.key();
         if (action == 1) {
            Alien.MODULE.onKeyPressed(key);
         }

         if (action == 0) {
            Alien.MODULE.onKeyReleased(key);
         }
      } catch (Exception var7) {
         var7.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage(ChatFormatting.DARK_RED + "[ERROR] onKey " + var7.getMessage());
         }
      }
   }

   @Redirect(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;isActive()Z"), require = 0)
   public boolean hook(GameNarrator instance) {
      return false;
   }
}
