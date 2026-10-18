package dev.luminous.api.utils.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

public final class EntityOverlayRenderer {
   private static TextureTarget throughWallDepth;

   private EntityOverlayRenderer() {
   }

   public static void flushThroughWall() {
      Minecraft mc = Minecraft.getInstance();
      int width = mc.getWindow().getWidth();
      int height = mc.getWindow().getHeight();
      if (width > 0 && height > 0) {
         if (!mc.gameRenderer.getSubmitNodeStorage().getSubmitsPerOrder().values().stream().noneMatch(queue -> queue.wasUsed())) {
            if (throughWallDepth == null || throughWallDepth.width != width || throughWallDepth.height != height) {
               if (throughWallDepth != null) {
                  throughWallDepth.destroyBuffers();
               }

               throughWallDepth = new TextureTarget("alien_entity_overlay_depth", width, height, true);
            }

            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(throughWallDepth.getDepthTexture(), 1.0);
            GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;
            RenderSystem.outputDepthTextureOverride = throughWallDepth.getDepthTextureView();

            try {
               mc.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
            } finally {
               RenderSystem.outputDepthTextureOverride = previousDepthOverride;
            }
         }
      }
   }
}
