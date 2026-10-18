package dev.luminous.core.impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.luminous.api.utils.Wrapper;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class BlurManager implements Wrapper {
   private static final int UBO_SIZE = 112;
   private static final RenderPipeline BLUR_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder()
         .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/blur"))
         .withVertexShader(Identifier.fromNamespaceAndPath("alienclient", "core/blit"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("alienclient", "core/blur"))
         .withSampler("InSampler")
         .withUniform("AlienParams", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
         .build()
   );
   private static final RenderPipeline COPY_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder()
         .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/copy"))
         .withVertexShader(Identifier.fromNamespaceAndPath("alienclient", "core/blit"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("alienclient", "core/copy"))
         .withSampler("InSampler")
         .withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
         .withDepthWrite(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
         .build()
   );
   private RenderTarget blurBuffer;

   public void applyBlur(float radius, float startX, float startY, float width, float height) {
      RenderSystem.assertOnRenderThread();
      RenderTarget main = mc.getMainRenderTarget();
      if (main != null) {
         if (this.blurBuffer == null) {
            this.blurBuffer = new TextureTarget("alien_blur", main.width, main.height, false);
         }

         if (this.blurBuffer.width != main.width || this.blurBuffer.height != main.height) {
            this.blurBuffer.resize(main.width, main.height);
         }

         float factor = mc.getWindow().getGuiScale();

         try (
            RenderPass pass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "alien_blur_pass", this.blurBuffer.getColorTextureView(), OptionalInt.of(0));
            GpuBuffer uniforms = this.buildUniforms(radius, startX * factor, startY * factor, width * factor, height * factor);
         ) {
            pass.setPipeline(BLUR_PIPELINE);
            pass.setUniform("AlienParams", uniforms);
            pass.bindTexture("InSampler", main.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(0, 3);
         }

         GpuTextureView depthView = main.useDepth ? main.getDepthTextureView() : null;

         try (RenderPass pass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "alien_blur_copy", main.getColorTextureView(), OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(COPY_PIPELINE);
            pass.bindTexture("InSampler", this.blurBuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(0, 3);
         }
      }
   }

   private GpuBuffer buildUniforms(float radius, float x, float y, float w, float h) {
      Std140Builder builder = Std140Builder.intoBuffer(ByteBuffer.allocateDirect(112));
      builder.putVec4(x, y, w, h);
      builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      builder.putVec4(radius, 0.0F, 0.0F, 0.0F);
      builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      return RenderSystem.getDevice().createBuffer(() -> "alien_blur_uniforms", 128, builder.get());
   }
}
