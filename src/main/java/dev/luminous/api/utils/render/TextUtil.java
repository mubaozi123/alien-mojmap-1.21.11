package dev.luminous.api.utils.render;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.FontManager;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class TextUtil implements Wrapper {
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

   public static float getWidth(String s) {
      return mc.font.width(s);
   }

   public static float getHeight() {
      return 9.0F;
   }

   public static void drawStringWithScale(GuiGraphics drawContext, String text, float x, float y, int color, float scale) {
      Matrix3x2fStack matrixStack = drawContext.pose();
      if (scale != 1.0F) {
         matrixStack.pushMatrix();
         matrixStack.scale(scale, scale);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F);
         }
      }

      drawString(drawContext, text, x, y, color);
      matrixStack.popMatrix();
   }

   public static void drawStringScale(GuiGraphics drawContext, String text, float x, float y, int color, float scale, boolean shadow) {
      Matrix3x2fStack matrixStack = drawContext.pose();
      if (scale != 1.0F) {
         matrixStack.pushMatrix();
         matrixStack.scale(scale, scale);
         if (scale > 1.0F) {
            matrixStack.translate(-x / scale, -y / scale);
         } else {
            matrixStack.translate(x / scale / 2.0F, y / scale / 2.0F);
         }
      }

      drawContext.drawString(mc.font, text, (int)x, (int)y, color, shadow);
      matrixStack.popMatrix();
   }

   public static void drawString(GuiGraphics drawContext, String text, double x, double y, int color) {
      drawString(drawContext, text, x, y, color, false);
   }

   public static void drawString(GuiGraphics drawContext, String text, double x, double y, int color, boolean customFont) {
      drawString(drawContext, text, x, y, color, customFont, true);
   }

   public static void drawString(GuiGraphics drawContext, String text, double x, double y, int color, boolean customFont, boolean shadow) {
      if (customFont) {
         FontManager.ensureInitialized();
         if (FontManager.ui != null) {
            FontManager.ui.drawString(drawContext.pose(), text, (int)x, (int)y, color, shadow);
            return;
         }
      }

      drawContext.drawString(mc.font, text, (int)x, (int)y, color, shadow);
   }

   public static void drawStringPulse(
      GuiGraphics drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont
   ) {
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            s = c == 'r' ? null : "§" + c;
            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startColor.getRGB(), customFont);
            } else {
               drawString(drawContext, String.valueOf(c), x, y, ColorUtil.pulseColor(startColor, endColor, index, counter, speed).getRGB(), customFont);
            }

            x += customFont && FontManager.ui != null ? FontManager.ui.getWidth(String.valueOf(c)) : mc.font.width(String.valueOf(c));
         }
      }
   }

   public static void drawStringPulse(
      GuiGraphics drawContext, String text, double x, double y, Color startColor, Color endColor, double speed, int counter, boolean customFont, boolean shadow
   ) {
      char[] stringToCharArray = text.toCharArray();
      int index = 0;
      boolean color = false;
      String s = null;

      for (char c : stringToCharArray) {
         if (c == 167) {
            color = true;
         } else if (color) {
            s = c == 'r' ? null : "§" + c;
            color = false;
         } else {
            index++;
            if (s != null) {
               drawString(drawContext, s + c, x, y, startColor.getRGB(), customFont, shadow);
            } else {
               drawString(drawContext, String.valueOf(c), x, y, ColorUtil.pulseColor(startColor, endColor, index, counter, speed).getRGB(), customFont, shadow);
            }

            x += customFont && FontManager.ui != null ? FontManager.ui.getWidth(String.valueOf(c)) : mc.font.width(String.valueOf(c));
         }
      }
   }

   public static Vec3 worldSpaceToScreenSpace(Vec3 pos) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      int displayHeight = mc.getWindow().getScreenHeight();
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      Vector3f target = new Vector3f();
      double deltaX = pos.x - camera.position().x;
      double deltaY = pos.y - camera.position().y;
      double deltaZ = pos.z - camera.position().z;
      Vector4f transformedCoordinates = new Vector4f((float)deltaX, (float)deltaY, (float)deltaZ, 1.0F).mul(lastWorldSpaceMatrix);
      Matrix4f matrixProj = new Matrix4f(lastProjMat);
      Matrix4f matrixModel = new Matrix4f(lastModMat);
      matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
      return new Vec3((double)target.x / mc.getWindow().getGuiScale(), (double)(displayHeight - target.y) / mc.getWindow().getGuiScale(), target.z);
   }
}
