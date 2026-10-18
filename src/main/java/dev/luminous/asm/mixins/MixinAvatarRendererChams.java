package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.mod.modules.impl.render.Chams;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public class MixinAvatarRendererChams {
   @Redirect(
      method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/geom/ModelPart;Z)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
      )
   )
   private void submitChamsHand(
      SubmitNodeCollector orderedRenderCommandQueue,
      ModelPart modelPart,
      PoseStack matrixStack,
      RenderType renderLayer,
      int n,
      int n2,
      TextureAtlasSprite sprite
   ) {
      Chams chams = Chams.INSTANCE;
      if (chams != null && chams.isOn() && chams.hand.booleanValue) {
         orderedRenderCommandQueue.submitModelPart(modelPart, matrixStack, renderLayer, n, n2, sprite, false, false, chams.getHandArgb(), null, 0);
      } else {
         orderedRenderCommandQueue.submitModelPart(modelPart, matrixStack, renderLayer, n, n2, sprite);
      }
   }
}
