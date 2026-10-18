package dev.luminous.api.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.luminous.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.world.entity.Entity;

public class JelloUtil implements Wrapper {
   private static float prevCircleStep;
   private static float circleStep;

   public static void drawJello(PoseStack matrix, Entity target, Color color) {
      double cs = prevCircleStep + (circleStep - prevCircleStep) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      double prevSinAnim = absSinAnimation(cs - 0.45F);
      double sinAnim = absSinAnimation(cs);
      double x = target.xOld
         + (target.getX() - target.xOld) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)
         - mc.gameRenderer.getMainCamera().position().x();
      double y = target.yOld
         + (target.getY() - target.yOld) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)
         - mc.gameRenderer.getMainCamera().position().y()
         + prevSinAnim * target.getBbHeight();
      double z = target.zOld
         + (target.getZ() - target.zOld) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)
         - mc.gameRenderer.getMainCamera().position().z();
      double nextY = target.yOld
         + (target.getY() - target.yOld) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)
         - mc.gameRenderer.getMainCamera().position().y()
         + sinAnim * target.getBbHeight();
      matrix.pushPose();
      GlStateManager._enableBlend();
      GlStateManager._disableCull();
      GlStateManager._disableDepthTest();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i <= 30; i++) {
         float cos = (float)(
            x
               + Math.cos(i * 6.28 / 30.0)
                  * (target.getBoundingBox().maxX - target.getBoundingBox().minX + (target.getBoundingBox().maxZ - target.getBoundingBox().minZ))
                  * 0.5
         );
         float sin = (float)(
            z
               + Math.sin(i * 6.28 / 30.0)
                  * (target.getBoundingBox().maxX - target.getBoundingBox().minX + (target.getBoundingBox().maxZ - target.getBoundingBox().minZ))
                  * 0.5
         );
         bufferBuilder.addVertex(matrix.last().pose(), cos, (float)nextY, sin).setColor(color.getRGB());
         bufferBuilder.addVertex(matrix.last().pose(), cos, (float)y, sin).setColor(ColorUtil.injectAlpha(color, 0).getRGB());
      }

      ImmediateDrawUtil.drawWorld(bufferBuilder, true);
      GlStateManager._enableCull();
      GlStateManager._disableBlend();
      GlStateManager._enableDepthTest();
      matrix.popPose();
   }

   public static void updateJello() {
      prevCircleStep = circleStep;
      circleStep += 0.15F;
   }

   private static double absSinAnimation(double input) {
      return Math.abs(1.0 + Math.sin(input)) / 2.0;
   }
}
