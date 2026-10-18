package dev.luminous.asm.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface IItemRenderer {
   @Invoker("renderItem")
   static void hookRenderItem(
      ItemDisplayContext displayContext,
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      int overlay,
      int[] tints,
      List<BakedQuad> quads,
      RenderType layer,
      FoilType glint
   ) {
   }

   @Invoker("renderQuadList")
   static void hookRenderBakedItemQuads(PoseStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, int light, int overlay) {
   }
}
