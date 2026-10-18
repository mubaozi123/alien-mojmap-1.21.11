package dev.luminous.asm.mixins;

import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   protected Minecraft minecraft;

   @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
   public void renderInGameBackgroundHook(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      ci.cancel();
      Matrix3x2fStack stack = context.pose();
      if (this.minecraft.level == null) {
         this.renderPanorama(context, delta);
      }

      if (ClientSetting.INSTANCE.darkening.getValue()) {
         Render2DUtil.rectFilled(stack, 0.0F, 0.0F, this.width, this.height, 1342177280);
      }

      if (this.minecraft.level != null && ClickGui.getInstance().tint.booleanValue) {
         Render2DUtil.verticalGradient(
            stack, 0.0F, 0.0F, this.width, this.height, ClickGui.getInstance().tint.getValue().getRGB(), ClickGui.getInstance().endColor.getValue().getRGB()
         );
      }
   }

   @Shadow
   protected void renderPanorama(GuiGraphics context, float delta) {
   }

   @Shadow
   protected void renderMenuBackground(GuiGraphics context) {
   }

   @Shadow
   public void onClose() {
   }

   @Shadow
   public ScreenRectangle getRectangle() {
      return null;
   }

   @Shadow
   protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement) {
      return null;
   }
}
