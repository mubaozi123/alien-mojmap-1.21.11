package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.api.utils.render.HandTintUtil;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
   @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, index = 2)
   private static MultiBufferSource alien$tintHandItem(
      MultiBufferSource provider,
      ItemDisplayContext displayContext,
      PoseStack matrices,
      MultiBufferSource originalProvider,
      int light,
      int overlay,
      int[] tints,
      List<BakedQuad> quads,
      RenderType layer,
      FoilType glint
   ) {
      Chams chams = Chams.INSTANCE;
      return chams != null && chams.isOn() && chams.hand.booleanValue && displayContext.firstPerson()
         ? HandTintUtil.wrap(provider, chams.hand.getValue())
         : provider;
   }

   @Inject(method = "renderItem", at = @At("HEAD"))
   private static void filterGroundItemQuads(
      ItemDisplayContext displayContext,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      int overlay,
      int[] tints,
      List<BakedQuad> quads,
      RenderType layer,
      FoilType glint,
      CallbackInfo ci
   ) {
      if (NoRender.INSTANCE.isOn()
         && NoRender.INSTANCE.fastItem.getValue()
         && !NoRender.INSTANCE.renderSidesOfItems.getValue()
         && displayContext == ItemDisplayContext.GROUND) {
         List<BakedQuad> filtered = new ArrayList<>();

         for (BakedQuad quad : quads) {
            if (quad.direction() == null) {
               filtered.add(quad);
            }
         }

         quads.clear();
         quads.addAll(filtered);
      }
   }
}
