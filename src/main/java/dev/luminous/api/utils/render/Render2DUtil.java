package dev.luminous.api.utils.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.luminous.api.utils.Wrapper;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

public class Render2DUtil implements Wrapper {
   public static void rect(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void arrow(Matrix3x2fStack matrixStack, float x, float y, Color color) {
      drawRectWithOutline(matrixStack, x - 1.0F, y - 1.0F, 2.0F, 2.0F, color, Color.BLACK);
   }

   public static void rectFilled(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int color) {
      float f = (color >> 24 & 0xFF) / 255.0F;
      float g = (color >> 16 & 0xFF) / 255.0F;
      float h = (color >> 8 & 0xFF) / 255.0F;
      float j = (color & 0xFF) / 255.0F;
      if (!(f <= 0.01)) {
         if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
         }

         if (y1 < y2) {
            float i = y1;
            y1 = y2;
            y2 = i;
         }

         BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         bufferBuilder.addVertexWith2DPose(stack, x1, y2).setColor(g, h, j, f);
         bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(g, h, j, f);
         bufferBuilder.addVertexWith2DPose(stack, x2, y1).setColor(g, h, j, f);
         bufferBuilder.addVertexWith2DPose(stack, x1, y1).setColor(g, h, j, f);
         ImmediateDrawUtil.drawGui(bufferBuilder);
      }
   }

   public static void horizontalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, x1, y1).setColor(startColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x1, y2).setColor(startColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(endColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x2, y1).setColor(endColor.getRGB());
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void horizontalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, x1, y1).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x1, y2).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(endColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y1).setColor(endColor);
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void verticalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, x1, y1).setColor(startColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x2, y1).setColor(startColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(endColor.getRGB());
      bufferBuilder.addVertexWith2DPose(stack, x1, y2).setColor(endColor.getRGB());
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void verticalGradient(Matrix3x2fStack stack, float x1, float y1, float x2, float y2, int startColor, int endColor) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, x1, y1).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y1).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(endColor);
      bufferBuilder.addVertexWith2DPose(stack, x1, y2).setColor(endColor);
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void drawLine(Matrix3x2fStack stack, float x, float y, float x1, float y1, int color) {
      float thickness = 0.5F;
      float dx = x1 - x;
      float dy = y1 - y;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-4F)) {
         float nx = -dy / len * 0.25F;
         float ny = dx / len * 0.25F;
         BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
         bufferBuilder.addVertexWith2DPose(stack, x + nx, y + ny).setColor(color);
         bufferBuilder.addVertexWith2DPose(stack, x - nx, y - ny).setColor(color);
         bufferBuilder.addVertexWith2DPose(stack, x1 + nx, y1 + ny).setColor(color);
         bufferBuilder.addVertexWith2DPose(stack, x1 - nx, y1 - ny).setColor(color);
         ImmediateDrawUtil.drawGui(bufferBuilder);
      }
   }

   public static void drawRectWithOutline(Matrix3x2fStack stack, float x, float y, float width, float height, Color c, Color c2) {
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.addVertexWith2DPose(stack, x, y + height).setColor(c.getRGB());
      buffer.addVertexWith2DPose(stack, x + width, y + height).setColor(c.getRGB());
      buffer.addVertexWith2DPose(stack, x + width, y).setColor(c.getRGB());
      buffer.addVertexWith2DPose(stack, x, y).setColor(c.getRGB());
      ImmediateDrawUtil.drawGui(buffer);
      buffer = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      buffer.addVertexWith2DPose(stack, x, y + height).setColor(c2.getRGB());
      buffer.addVertexWith2DPose(stack, x + width, y + height).setColor(c2.getRGB());
      buffer.addVertexWith2DPose(stack, x + width, y).setColor(c2.getRGB());
      buffer.addVertexWith2DPose(stack, x, y).setColor(c2.getRGB());
      buffer.addVertexWith2DPose(stack, x, y + height).setColor(c2.getRGB());
      ImmediateDrawUtil.drawGui(buffer);
   }

   public static void drawRect(Matrix3x2fStack stack, float x, float y, float width, float height, int c) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, x, y + height).setColor(c);
      bufferBuilder.addVertexWith2DPose(stack, x + width, y + height).setColor(c);
      bufferBuilder.addVertexWith2DPose(stack, x + width, y).setColor(c);
      bufferBuilder.addVertexWith2DPose(stack, x, y).setColor(c);
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void drawRect(Matrix3x2fStack stack, float x, float y, float width, float height, Color c) {
      drawRect(stack, x, y, width, height, c.getRGB());
   }

   public static void drawRect(GuiGraphics drawContext, float x, float y, float width, float height, Color c) {
      drawRect(drawContext.pose(), x, y, width, height, c);
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
   }

   public static void drawGlow(Matrix3x2fStack stack, float x, float y, float width, float height, int color) {
      int startColor = ColorUtil.injectAlpha(color, 20);
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float halfWidth = width / 2.0F;
      float halfHeight = height / 2.0F;
      float centerX = x + halfWidth;
      float centerY = y + halfHeight;
      float x2 = x + width;
      float y2 = y + height;
      bufferBuilder.addVertexWith2DPose(stack, centerX, centerY).setColor(color);
      bufferBuilder.addVertexWith2DPose(stack, x, centerY).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x, y).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, y).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, centerY).setColor(color);
      bufferBuilder.addVertexWith2DPose(stack, centerX, y).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, centerY).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, centerY).setColor(color);
      bufferBuilder.addVertexWith2DPose(stack, x, centerY).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x, y2).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, y2).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, centerY).setColor(color);
      bufferBuilder.addVertexWith2DPose(stack, x2, centerY).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, x2, y2).setColor(startColor);
      bufferBuilder.addVertexWith2DPose(stack, centerX, y2).setColor(startColor);
      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void drawCircle(Matrix3x2fStack stack, float cx, float cy, float r, Color c, int segments) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertexWith2DPose(stack, cx, cy).setColor(c.getRGB());

      for (int i = 0; i <= segments; i++) {
         double keyCodec = i * (Math.PI * 2) / segments;
         float x = (float)(cx + Math.cos(keyCodec) * r);
         float y = (float)(cy + Math.sin(keyCodec) * r);
         bufferBuilder.addVertexWith2DPose(stack, x, y).setColor(c.getRGB());
      }

      ImmediateDrawUtil.drawGui(bufferBuilder);
   }

   public static void drawPill(Matrix3x2fStack stack, float x, float y, float width, float height, Color c) {
      float r = height / 2.0F;
      drawRect(stack, x + r, y, width - 2.0F * r, height, c);
      drawCircle(stack, x + r, y + r, r, c, 64);
      drawCircle(stack, x + width - r, y + r, r, c, 64);
   }

   public static void drawRoundedStroke(Matrix3x2fStack stack, float x, float y, float width, float height, float radius, Color c, int seg) {
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;

      for (int i = 0; i <= seg; i++) {
         double keyCodec = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
         buffer.addVertexWith2DPose(stack, (float)(x2 + Math.cos(keyCodec) * r), (float)(y1 + Math.sin(keyCodec) * r)).setColor(c.getRGB());
      }

      for (int var20 = 0; var20 <= seg; var20++) {
         double keyCodec = 0.0 + var20 * (Math.PI / 2) / seg;
         buffer.addVertexWith2DPose(stack, (float)(x2 + Math.cos(keyCodec) * r), (float)(y2 + Math.sin(keyCodec) * r)).setColor(c.getRGB());
      }

      for (int var21 = 0; var21 <= seg; var21++) {
         double keyCodec = (Math.PI / 2) + var21 * (Math.PI / 2) / seg;
         buffer.addVertexWith2DPose(stack, (float)(x1 + Math.cos(keyCodec) * r), (float)(y2 + Math.sin(keyCodec) * r)).setColor(c.getRGB());
      }

      for (int var22 = 0; var22 <= seg; var22++) {
         double keyCodec = Math.PI + var22 * (Math.PI / 2) / seg;
         buffer.addVertexWith2DPose(stack, (float)(x1 + Math.cos(keyCodec) * r), (float)(y1 + Math.sin(keyCodec) * r)).setColor(c.getRGB());
      }

      ImmediateDrawUtil.drawGui(buffer);
   }

   public static void drawRainbowRoundedStroke(
      Matrix3x2fStack stack, float x, float y, float width, float height, float radius, int seg, float speed, int alpha
   ) {
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      float r = Math.min(radius, Math.min(width, height) / 2.0F);
      float x1 = x + r;
      float y1 = y + r;
      float x2 = x + width - r;
      float y2 = y + height - r;
      double t = (double)(System.currentTimeMillis() % (long)(1000.0F / Math.max(0.001F, speed))) / (1000.0F / Math.max(0.001F, speed));

      for (int i2 = 0; i2 <= seg; i2++) {
         double keyCodec = (-Math.PI / 2) + i2 * (Math.PI / 2) / seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.addVertexWith2DPose(stack, (float)(x2 + Math.cos(keyCodec) * r), (float)(y1 + Math.sin(keyCodec) * r)).setColor(argb2);
      }

      for (int var52 = 0; var52 <= seg; var52++) {
         double keyCodec = 0.0 + var52 * (Math.PI / 2) / seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.addVertexWith2DPose(stack, (float)(x2 + Math.cos(keyCodec) * r), (float)(y2 + Math.sin(keyCodec) * r)).setColor(argb2);
      }

      for (int var53 = 0; var53 <= seg; var53++) {
         double keyCodec = (Math.PI / 2) + var53 * (Math.PI / 2) / seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.addVertexWith2DPose(stack, (float)(x1 + Math.cos(keyCodec) * r), (float)(y2 + Math.sin(keyCodec) * r)).setColor(argb2);
      }

      for (int var54 = 0; var54 <= seg; var54++) {
         double keyCodec = Math.PI + var54 * (Math.PI / 2) / seg;
         float hue = (float)((keyCodec + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb2 = Color.HSBtoRGB(hue, 1.0F, 1.0F);
         int argb2 = alpha << 24 | rgb2 & 16777215;
         buffer.addVertexWith2DPose(stack, (float)(x1 + Math.cos(keyCodec) * r), (float)(y1 + Math.sin(keyCodec) * r)).setColor(argb2);
      }

      ImmediateDrawUtil.drawGui(buffer);
      BufferBuilder inner = Tesselator.getInstance().begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      float rIn = Math.max(0.5F, r - 1.2F);

      for (int i = 0; i <= seg; i++) {
         double a2 = (-Math.PI / 2) + i * (Math.PI / 2) / seg;
         float hue2 = (float)((a2 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue2, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.addVertexWith2DPose(stack, (float)(x2 + Math.cos(a2) * rIn), (float)(y1 + Math.sin(a2) * rIn)).setColor(argb);
      }

      for (int var37 = 0; var37 <= seg; var37++) {
         double a3 = 0.0 + var37 * (Math.PI / 2) / seg;
         float hue3 = (float)((a3 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue3, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.addVertexWith2DPose(stack, (float)(x2 + Math.cos(a3) * rIn), (float)(y2 + Math.sin(a3) * rIn)).setColor(argb);
      }

      for (int var38 = 0; var38 <= seg; var38++) {
         double a4 = (Math.PI / 2) + var38 * (Math.PI / 2) / seg;
         float hue4 = (float)((a4 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue4, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.addVertexWith2DPose(stack, (float)(x1 + Math.cos(a4) * rIn), (float)(y2 + Math.sin(a4) * rIn)).setColor(argb);
      }

      for (int var39 = 0; var39 <= seg; var39++) {
         double a5 = Math.PI + var39 * (Math.PI / 2) / seg;
         float hue5 = (float)((a5 + (Math.PI * 2)) / (Math.PI * 2) + t) % 1.0F;
         int rgb = Color.HSBtoRGB(hue5, 1.0F, 1.0F);
         int argb = alpha << 24 | rgb & 16777215;
         inner.addVertexWith2DPose(stack, (float)(x1 + Math.cos(a5) * rIn), (float)(y1 + Math.sin(a5) * rIn)).setColor(argb);
      }

      ImmediateDrawUtil.drawGui(inner);
   }

   public static void drawRoundedRect(Matrix3x2fStack stack, float x, float y, float width, float height, float radius, Color c) {
      if (radius <= 0.0F) {
         drawRect(stack, x, y, width, height, c);
      } else {
         float r = Math.min(radius, Math.min(width, height) / 2.0F);
         drawRect(stack, x + r, y, width - 2.0F * r, height, c);
         drawRect(stack, x, y + r, r, height - 2.0F * r, c);
         drawRect(stack, x + width - r, y + r, r, height - 2.0F * r, c);
         int seg = 48;
         float cx = x + r;
         float cy = y + r;
         BufferBuilder buffer = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         buffer.addVertexWith2DPose(stack, cx, cy).setColor(c.getRGB());

         for (int i = 0; i <= seg; i++) {
            double keyCodec = Math.PI + i * (Math.PI / 2) / seg;
            buffer.addVertexWith2DPose(stack, (float)(cx + Math.cos(keyCodec) * r), (float)(cy + Math.sin(keyCodec) * r)).setColor(c.getRGB());
         }

         ImmediateDrawUtil.drawGui(buffer);
         cx = x + width - r;
         cy = y + r;
         buffer = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         buffer.addVertexWith2DPose(stack, cx, cy).setColor(c.getRGB());

         for (int var18 = 0; var18 <= seg; var18++) {
            double keyCodec = (Math.PI * 3.0 / 2.0) + var18 * (Math.PI / 2) / seg;
            buffer.addVertexWith2DPose(stack, (float)(cx + Math.cos(keyCodec) * r), (float)(cy + Math.sin(keyCodec) * r)).setColor(c.getRGB());
         }

         ImmediateDrawUtil.drawGui(buffer);
         cx = x + width - r;
         cy = y + height - r;
         buffer = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         buffer.addVertexWith2DPose(stack, cx, cy).setColor(c.getRGB());

         for (int var19 = 0; var19 <= seg; var19++) {
            double keyCodec = 0.0 + var19 * (Math.PI / 2) / seg;
            buffer.addVertexWith2DPose(stack, (float)(cx + Math.cos(keyCodec) * r), (float)(cy + Math.sin(keyCodec) * r)).setColor(c.getRGB());
         }

         ImmediateDrawUtil.drawGui(buffer);
         cx = x + r;
         cy = y + height - r;
         buffer = Tesselator.getInstance().begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         buffer.addVertexWith2DPose(stack, cx, cy).setColor(c.getRGB());

         for (int var20 = 0; var20 <= seg; var20++) {
            double keyCodec = (Math.PI / 2) + var20 * (Math.PI / 2) / seg;
            buffer.addVertexWith2DPose(stack, (float)(cx + Math.cos(keyCodec) * r), (float)(cy + Math.sin(keyCodec) * r)).setColor(c.getRGB());
         }

         ImmediateDrawUtil.drawGui(buffer);
      }
   }

   public static void drawDropShadow(Matrix3x2fStack matrices, float x, float y, float width, float height, float radius) {
   }
}
