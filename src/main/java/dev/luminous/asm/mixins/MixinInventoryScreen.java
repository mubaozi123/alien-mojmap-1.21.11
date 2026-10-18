package dev.luminous.asm.mixins;

import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
   @Unique
   private static LivingEntity alien$previewEntity;

   @Inject(
      method = "renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
      at = @At("HEAD")
   )
   private static void alien$capturePreviewEntity(
      GuiGraphics context, int x1, int y1, int x2, int y2, int size, float scale, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci
   ) {
      alien$previewEntity = entity;
   }

   @ModifyArgs(
      method = "renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;submitEntityRenderState(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V"
      )
   )
   private static void alien$applyRenderRotation(Args args) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && alien$previewEntity == mc.player && ClientSetting.INSTANCE.rotations.getValue()) {
         EntityRenderState state = (EntityRenderState)args.get(0);
         if (state instanceof LivingEntityRenderState livingState) {
            float bodyYaw = RotationManager.getRenderYawOffset();
            livingState.bodyRot = bodyYaw;
            livingState.yRot = Mth.wrapDegrees(RotationManager.getRotationYawHead() - bodyYaw);
            livingState.xRot = RotationManager.getRenderPitch();
         }
      }
   }
}
