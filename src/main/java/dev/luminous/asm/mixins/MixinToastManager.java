package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class MixinToastManager {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void hookDraw(GuiGraphics context, CallbackInfo ci) {
      if (NoRender.INSTANCE.guiToast.getValue()) {
         ci.cancel();
      }
   }
}
