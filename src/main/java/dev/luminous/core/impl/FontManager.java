package dev.luminous.core.impl;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.joml.Matrix3x2fStack;

public class FontManager {
   public static FontRenderer ui;
   public static FontRenderer small;
   public static FontRenderer icon;

   public static synchronized void ensureInitialized() {
      if (ui == null || small == null || icon == null) {
         if (Wrapper.mc != null && Wrapper.mc.getWindow() != null) {
            init();
         }
      }
   }

   public static synchronized void init() {
      if (ui == null || small == null || icon == null) {
         try {
            FontRenderer nextUi = assets(8.0F, "default", 0);
            FontRenderer nextSmall = assets(6.0F, "default", 0);
            FontRenderer nextIcon = assetsWithoutOffset(8.0F, "icon", 0);
            ui = nextUi;
            small = nextSmall;
            icon = nextIcon;
         } catch (Exception var4) {
            System.err.println("[Alien] Custom fonts unavailable, using system fallback: " + var4.getMessage());
            Font fallback = new Font("SansSerif", 0, 8);
            Font smallFallback = fallback.deriveFont(6.0F);
            Font iconFallback = new Font("SansSerif", 0, 8);
            ui = new FontRenderer(fallback, 8.0F);
            small = new FontRenderer(smallFallback, 6.0F);
            icon = new FontRenderer(iconFallback, 8.0F);
         }
      }
   }

   public static FontRenderer assets(float size, String font, int style, String alternate) throws IOException, FontFormatException {
      ClassLoader classLoader = FontManager.class.getClassLoader();
      InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + font + ".ttf");
      InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
      InputStream stream = primary != null ? primary : fallback;
      if (stream == null) {
         stream = FontManager.class.getResourceAsStream("/assets/minecraft/font/default.ttf");
      }

      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), getFont(alternate, style, (int)size), size) {
         @Override
         public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer assetsWithoutOffset(float size, String name, int style) throws IOException, FontFormatException {
      ClassLoader classLoader = FontManager.class.getClassLoader();
      InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + name + ".ttf");
      InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
      InputStream stream = primary != null ? primary : fallback;
      if (stream == null) {
         stream = FontManager.class.getResourceAsStream("/assets/minecraft/font/default.ttf");
      }

      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), size);
   }

   public static FontRenderer assets(float size, String name, int style) throws IOException, FontFormatException {
      ClassLoader classLoader = FontManager.class.getClassLoader();
      InputStream primary = classLoader.getResourceAsStream("assets/alienclient/font/" + name + ".ttf");
      InputStream fallback = classLoader.getResourceAsStream("assets/minecraft/font/default.ttf");
      InputStream stream = primary != null ? primary : fallback;
      return new FontRenderer(Font.createFont(0, Objects.requireNonNull(stream)).deriveFont(style, size), size) {
         @Override
         public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style, String alternate) {
      return new FontRenderer(getFont(font, style, size), getFont(alternate, style, size), size) {
         @Override
         public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style) {
      return new FontRenderer(getFont(font, style, size), size) {
         @Override
         public void drawString(Matrix3x2fStack stack, String s, float x, float y, float r, float g, float elementCodec, float keyCodec, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, elementCodec, keyCodec, shadow);
         }
      };
   }

   private static Font getFont(String font, int style, int size) {
      File fontDir = new File("C:\\Windows\\Fonts");

      try {
         for (File file : fontDir.listFiles()) {
            if (file.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(font)) {
               try {
                  return Font.createFont(0, file).deriveFont(style, size);
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         }

         for (File filex : fontDir.listFiles()) {
            if (filex.getName().startsWith(font)) {
               try {
                  return Font.createFont(0, filex).deriveFont(style, size);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }
         }
      } catch (Exception var11) {
      }

      return new Font(null, style, size);
   }
}
