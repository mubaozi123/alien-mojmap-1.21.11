package dev.luminous.api.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class HandTintUtil {
   private HandTintUtil() {
   }

   public static MultiBufferSource wrap(MultiBufferSource provider, Color color) {
      final float red = color.getRed() / 255.0F;
      final float green = color.getGreen() / 255.0F;
      final float blue = color.getBlue() / 255.0F;
      final float alpha = color.getAlpha() / 255.0F;
      return new MultiBufferSource() {
         @Override
         public VertexConsumer getBuffer(RenderType layer) {
            return new HandTintUtil.TintingVertexConsumer(provider.getBuffer(layer), red, green, blue, alpha);
         }
      };
   }

   private static final class TintingVertexConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final float red;
      private final float green;
      private final float blue;
      private final float alpha;

      private TintingVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
         this.delegate = delegate;
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alpha = alpha;
      }

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.delegate.setColor(Math.round(red * this.red), Math.round(green * this.green), Math.round(blue * this.blue), Math.round(alpha * this.alpha));
         return this;
      }

      @Override
      public VertexConsumer setColor(int argb) {
         int a = argb >>> 24 & 0xFF;
         int r = argb >>> 16 & 0xFF;
         int g = argb >>> 8 & 0xFF;
         int b = argb & 0xFF;
         return this.setColor(r, g, b, a);
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.delegate.setUv1(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.delegate.setUv2(u, v);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         this.delegate.setNormal(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float width) {
         this.delegate.setLineWidth(width);
         return this;
      }
   }
}
