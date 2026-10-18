package dev.luminous.asm.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luminous.Alien;
import dev.luminous.api.interfaces.IMouseHook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouse implements IMouseHook {
   @Shadow
   private boolean mouseGrabbed;
   @Final
   @Shadow
   private Minecraft minecraft;
   @Shadow
   private double xpos;
   @Shadow
   private double ypos;

   @Inject(method = "onButton", at = @At("HEAD"))
   private void onMouse(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
      int key = -(input.button() + 2);
      if (action == 1) {
         Alien.MODULE.onKeyPressed(key);
      }

      if (action == 0) {
         Alien.MODULE.onKeyReleased(key);
      }
   }

   @Override
   public void alienClient$lock() {
      if (this.minecraft.isWindowActive() && !this.mouseGrabbed) {
         KeyMapping.setAll();
         this.mouseGrabbed = true;
         this.xpos = this.minecraft.getWindow().getScreenWidth() / 2.0;
         this.ypos = this.minecraft.getWindow().getScreenHeight() / 2.0;
         InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212995, this.xpos, this.ypos);
      }
   }
}
