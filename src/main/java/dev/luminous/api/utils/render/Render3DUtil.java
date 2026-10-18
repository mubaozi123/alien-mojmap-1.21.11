package dev.luminous.api.utils.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Render3DUtil implements Wrapper {
   public static void endBuilding(BufferBuilder bb) {
      ImmediateDrawUtil.drawWorld(bb, true);
   }

   public static void endBuilding(BufferBuilder bb, boolean depthTest) {
      ImmediateDrawUtil.drawWorld(bb, !depthTest);
   }

   public static PoseStack matrixFrom(double x, double y, double z) {
      PoseStack matrices = new PoseStack();
      Camera camera = mc.gameRenderer.getMainCamera();
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
      matrices.translate(x - camera.position().x, y - camera.position().y, z - camera.position().z);
      return matrices;
   }

   public static void drawText3D(String text, Vec3 vec3d, Color color) {
      drawText3D(Component.nullToEmpty(text), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color.getRGB());
   }

   public static void drawText3D(String text, Vec3 vec3d, int color) {
      drawText3D(Component.nullToEmpty(text), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 1.0, color);
   }

   public static void drawText3D(Component text, Vec3 vec3d, double offX, double offY, double scale, Color color) {
      drawText3D(text, vec3d.x, vec3d.y, vec3d.z, offX, offY, scale, color.getRGB());
   }

   public static void drawText3D(Component text, double x, double y, double z, double offX, double offY, double scale, int color) {
      PoseStack matrices = matrixFrom(x, y, z);
      Camera camera = mc.gameRenderer.getMainCamera();
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.translate(offX, offY, 0.0);
      matrices.scale(-0.025F * (float)scale, -0.025F * (float)scale, 1.0F);
      int halfWidth = mc.font.width(text) / 2;
      BufferSource immediate = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
      mc.font.drawInBatch(text.getString(), (float)(-halfWidth), 0.0F, color, true, matrices.last().pose(), immediate, DisplayMode.SEE_THROUGH, 0, 15728880);
      immediate.endBatch();
   }

   public static void drawFill(PoseStack matrixStack, AABB bb, Color fillColor) {
      draw3DBox(matrixStack, bb, fillColor, new Color(0, 0, 0, 0), false, true);
   }

   public static void drawBox(PoseStack matrixStack, AABB bb, Color outlineColor) {
      draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false);
   }

   public static void drawBox(PoseStack matrixStack, AABB bb, Color outlineColor, float lineWidth) {
      draw3DBox(matrixStack, bb, new Color(0, 0, 0, 0), outlineColor, true, false, lineWidth);
   }

   public static void draw3DBox(PoseStack matrixStack, AABB box, Color fillColor, Color outlineColor) {
      draw3DBox(matrixStack, box, fillColor, outlineColor, true, true);
   }

   public static void draw3DBox(PoseStack matrixStack, AABB box, Color fillColor, Color outlineColor, boolean outline, boolean fill) {
      draw3DBox(matrixStack, box, fillColor, outlineColor, outline, fill, 1.5F);
   }

   public static void draw3DBox(PoseStack matrixStack, AABB box, Color fillColor, Color outlineColor, boolean outline, boolean fill, float lineWidth) {
      draw3DBox(matrixStack, box, fillColor, outlineColor, outline, fill, lineWidth, false);
   }

   public static void draw3DBox(
      PoseStack matrixStack, AABB box, Color fillColor, Color outlineColor, boolean outline, boolean fill, float lineWidth, boolean depthTest
   ) {
      box = box.move(mc.gameRenderer.getMainCamera().position().reverse());
      Matrix4f matrix = matrixStack.last().pose();
      if (outline) {
         float a = outlineColor.getAlpha() / 255.0F;
         float r = outlineColor.getRed() / 255.0F;
         float g = outlineColor.getGreen() / 255.0F;
         float b = outlineColor.getBlue() / 255.0F;
         BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         ImmediateDrawUtil.drawWorld(bufferBuilder, !depthTest);
      }

      if (fill) {
         float a = fillColor.getAlpha() / 255.0F;
         float r = fillColor.getRed() / 255.0F;
         float g = fillColor.getGreen() / 255.0F;
         float b = fillColor.getBlue() / 255.0F;
         BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).setColor(r, g, b, a);
         bufferBuilder.addVertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).setColor(r, g, b, a);
         ImmediateDrawUtil.drawWorld(bufferBuilder, !depthTest);
      }
   }

   public static void drawFadeFill(PoseStack stack, AABB box, Color c, Color c1) {
      drawFadeFill(stack, box, c, c1, false);
   }

   public static void drawFadeFill(PoseStack stack, AABB box, Color c, Color c1, boolean depthTest) {
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f posMatrix = stack.last().pose();
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.position().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.position().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.position().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.position().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.position().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.position().z());
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, minY, minZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, minY, maxZ).setColor(c.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, minZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, minX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, maxZ).setColor(c1.getRGB());
      buffer.addVertex(posMatrix, maxX, maxY, minZ).setColor(c1.getRGB());
      ImmediateDrawUtil.drawWorld(buffer, !depthTest);
   }

   public static void drawLine(PoseStack matrices, Vec3 start, Vec3 end, Color color, float width) {
      drawLine(matrices, start, end, color, width, false);
   }

   public static void drawLine(PoseStack matrices, Vec3 start, Vec3 end, Color color, float width, boolean depthTest) {
      if (!(start.distanceToSqr(end) < 1.0E-6)) {
         BufferBuilder buffer = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
         vertexLine(matrices, buffer, start.x, start.y, start.z, end.x, end.y, end.z, color, width);
         ImmediateDrawUtil.drawWorld(buffer, !depthTest);
      }
   }

   public static void drawLine(Vec3 start, Vec3 end, Color color) {
      drawLine(start.x, start.y(), start.z, end.x(), end.y(), end.z(), color, 1.0F);
   }

   public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, float width) {
      drawLine(x1, y1, z1, x2, y2, z2, color, width, false);
   }

   public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color, float width, boolean depthTest) {
      PoseStack matrices = matrixFrom(x1, y1, z1);
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
      vertexLine(matrices, buffer, 0.0, 0.0, 0.0, x2 - x1, y2 - y1, z2 - z1, color, width);
      ImmediateDrawUtil.drawWorld(buffer, !depthTest);
   }

   public static void vertexLine(
      PoseStack matrices, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, Color lineColor, float lineWidth
   ) {
      Matrix4f model = matrices.last().pose();
      Pose entry = matrices.last();
      Vector3f normalVec = getNormal((float)x1, (float)y1, (float)z1, (float)x2, (float)y2, (float)z2);
      buffer.addVertex(model, (float)x1, (float)y1, (float)z1)
         .setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
         .setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z())
         .setLineWidth(lineWidth);
      buffer.addVertex(model, (float)x2, (float)y2, (float)z2)
         .setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha())
         .setNormal(entry, normalVec.x(), normalVec.y(), normalVec.z())
         .setLineWidth(lineWidth);
   }

   public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
      float xNormal = x2 - x1;
      float yNormal = y2 - y1;
      float zNormal = z2 - z1;
      float normalSqrt = Mth.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
      return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
   }

   public static void drawRect(PoseStack matrices, float x, float y, float width, float height, int c) {
      Matrix4f matrix = matrices.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix, x, y + height, 0.0F).setColor(c);
      bufferBuilder.addVertex(matrix, x + width, y + height, 0.0F).setColor(c);
      bufferBuilder.addVertex(matrix, x + width, y, 0.0F).setColor(c);
      bufferBuilder.addVertex(matrix, x, y, 0.0F).setColor(c);
      ImmediateDrawUtil.drawWorld(bufferBuilder, true);
   }

   public static void drawTargetEsp(PoseStack stack, @NotNull Entity target, Color color) {
      drawTargetEsp(stack, target, color, false);
   }

   public static void drawTargetEsp(PoseStack stack, @NotNull Entity target, Color color, boolean depthTest) {
      ArrayList<Vec3> vecs = new ArrayList<>();
      ArrayList<Vec3> vecs1 = new ArrayList<>();
      ArrayList<Vec3> vecs2 = new ArrayList<>();
      Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
      double x = target.xOld + (target.getX() - target.xOld) * getDynamicDeltaTicks() - cameraPos.x();
      double y = target.yOld + (target.getY() - target.yOld) * getDynamicDeltaTicks() - cameraPos.y();
      double z = target.zOld + (target.getZ() - target.zOld) * getDynamicDeltaTicks() - cameraPos.z();
      double height = target.getBbHeight();

      for (int i = 0; i <= 361; i++) {
         double v = Math.sin(Math.toRadians(i));
         double u = Math.cos(Math.toRadians(i));
         Vec3 vec = new Vec3((float)(u * 0.5), height, (float)(v * 0.5));
         vecs.add(vec);
         double v1 = Math.sin(Math.toRadians((i + 120) % 360));
         double u1 = Math.cos(Math.toRadians(i + 120) % 360.0);
         Vec3 vec1 = new Vec3((float)(u1 * 0.5), height, (float)(v1 * 0.5));
         vecs1.add(vec1);
         double v2 = Math.sin(Math.toRadians((i + 240) % 360));
         double u2 = Math.cos(Math.toRadians((i + 240) % 360));
         Vec3 vec2 = new Vec3((float)(u2 * 0.5), height, (float)(v2 * 0.5));
         vecs2.add(vec2);
         height -= 0.004F;
      }

      stack.pushPose();
      stack.translate(x, y, z);
      Matrix4f matrix = stack.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int j = 0; j < vecs.size() - 1; j++) {
         float alpha = 1.0F - (j + (float)(System.currentTimeMillis() - Alien.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs.get(j).x, (float)vecs.get(j).y, (float)vecs.get(j).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(j / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs.get(j + 1).x, (float)vecs.get(j + 1).y + 0.1F, (float)vecs.get(j + 1).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(j / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
      }

      endBuilding(bufferBuilder, depthTest);
      bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int var33 = 0; var33 < vecs1.size() - 1; var33++) {
         float alpha = 1.0F - (var33 + (float)(System.currentTimeMillis() - Alien.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs1.get(var33).x, (float)vecs1.get(var33).y, (float)vecs1.get(var33).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(var33 / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs1.get(var33 + 1).x, (float)vecs1.get(var33 + 1).y + 0.1F, (float)vecs1.get(var33 + 1).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(var33 / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
      }

      endBuilding(bufferBuilder, depthTest);
      bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int var34 = 0; var34 < vecs2.size() - 1; var34++) {
         float alpha = 1.0F - (var34 + (float)(System.currentTimeMillis() - Alien.initTime) / 5.0F) % 360.0F / 60.0F;
         bufferBuilder.addVertex(matrix, (float)vecs2.get(var34).x, (float)vecs2.get(var34).y, (float)vecs2.get(var34).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(var34 / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
         bufferBuilder.addVertex(matrix, (float)vecs2.get(var34 + 1).x, (float)vecs2.get(var34 + 1).y + 0.1F, (float)vecs2.get(var34 + 1).z)
            .setColor(ColorUtil.injectAlpha(ColorUtil.pulseColor(color, (int)(var34 / 20.0F), 10, 1.0), (int)(alpha * 255.0F)).getRGB());
      }

      endBuilding(bufferBuilder, depthTest);
      stack.popPose();
   }

   public static float getDynamicDeltaTicks() {
      return mc.getDeltaTracker().getGameTimeDeltaTicks();
   }
}
