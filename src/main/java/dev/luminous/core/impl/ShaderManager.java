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
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ShaderManager implements Wrapper {
   private static final int UBO_SIZE = 112;
   private static final String[] SHADER_NAMES = new String[]{"solid", "pulse", "smoke", "gradient", "snow", "flow", "rainbow"};
   private static final RenderPipeline[] PIPELINES = new RenderPipeline[ShaderManager.Shader.values().length];
   static final Timer timer = new Timer();
   private static final List<ShaderManager.RenderTask> tasks = new ArrayList<>();
   public float time = 0.0F;
   private RenderTarget shaderBuffer;

   public void renderShader(Runnable runnable, ShaderManager.Shader mode) {
      tasks.add(new ShaderManager.RenderTask(runnable, mode));
   }

   public void renderShaders() {
      tasks.forEach(t -> this.applyShader(t.task(), t.shader()));
      tasks.clear();
   }

   public void applyShader(Runnable runnable, ShaderManager.Shader mode) {
      if (!this.fullNullCheck()) {
         RenderSystem.assertOnRenderThread();
         RenderTarget mcBuffer = Minecraft.getInstance().getMainRenderTarget();
         if (this.shaderBuffer.width != mcBuffer.width || this.shaderBuffer.height != mcBuffer.height) {
            this.shaderBuffer.resize(mcBuffer.width, mcBuffer.height);
         }

         GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
         GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
         RenderSystem.outputColorTextureOverride = this.shaderBuffer.getColorTextureView();
         RenderSystem.outputDepthTextureOverride = this.shaderBuffer.getDepthTextureView();
         this.clearBuffer();

         try {
            runnable.run();
         } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
         }

         this.blitToScreen(mode, mcBuffer);
      }
   }

   private void clearBuffer() {
      RenderPass pass = RenderSystem.getDevice()
         .createCommandEncoder()
         .createRenderPass(
            () -> "alien_shader_clear",
            this.shaderBuffer.getColorTextureView(),
            OptionalInt.of(0),
            this.shaderBuffer.getDepthTextureView(),
            OptionalDouble.of(1.0)
         );
      if (pass != null) {
         pass.close();
      }
   }

   private void blitToScreen(ShaderManager.Shader mode, RenderTarget target) {
      GpuTextureView depthView = target.useDepth ? target.getDepthTextureView() : null;

      try (
         RenderPass pass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "alien_shader_blit", target.getColorTextureView(), OptionalInt.empty(), depthView, OptionalDouble.empty());
         GpuBuffer uniforms = this.buildUniforms(mode);
      ) {
         pass.setPipeline(PIPELINES[mode.ordinal()]);
         pass.setUniform("AlienParams", uniforms);
         pass.bindTexture("InSampler", this.shaderBuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(0, 3);
      }
   }

   private GpuBuffer buildUniforms(ShaderManager.Shader shader) {
      ShaderModule module = ShaderModule.INSTANCE;
      Color color = module.fill.getValue();
      this.time = (float)timer.getMs() / 5.0F * module.speed.getValueFloat() * 0.004F;
      int width = mc.getWindow().getGuiScaledWidth();
      int height = mc.getWindow().getGuiScaledHeight();
      float mixFactor = color.getAlpha() / 255.0F;
      float glowRadius = module.radius.getValueFloat();
      float glowQuality = module.smoothness.getValueFloat();
      float glowDivider = module.divider.getValueFloat();
      float glowMaxSample = module.maxSample.getValueFloat();
      Std140Builder builder = Std140Builder.intoBuffer(ByteBuffer.allocateDirect(112));
      switch (shader) {
         case Solid:
            this.putColor(builder, color, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(width, height, 0.0F, mixFactor);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(module.alpha.getValueFloat() / 255.0F, 0.0F, 0.0F, 0.0F);
            break;
         case Pulse:
            this.putColor(builder, color, 0.0F);
            this.putColor(builder, module.pulse.getValue(), 0.0F);
            builder.putVec4(width, height, this.time, mixFactor);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(module.alpha.getValueFloat() / 255.0F, module.pulseSpeed.getValueFloat(), 0.0F, 0.0F);
            break;
         case Smoke:
            this.putColor(builder, module.smoke1.getValue(), mixFactor);
            this.putColor(builder, module.smoke2.getValue(), 0.0F);
            builder.putVec4(width, height, this.time, 0.0F);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            this.putColor(builder, module.smoke3.getValue(), (float)module.octaves.getValue());
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            break;
         case Gradient:
            this.putColor(builder, module.smoke1.getValue(), mixFactor);
            this.putColor(builder, module.smoke2.getValue(), 0.0F);
            builder.putVec4(width, height, this.time * 30.0F, 0.0F);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            this.putColor(builder, module.smoke3.getValue(), (float)module.octaves.getValue());
            this.putColor(builder, module.smoke4.getValue(), 0.0F);
            builder.putVec4(module.step.getValueFloat(), 1.0F, 0.0F, 0.0F);
            break;
         case Snow:
            this.putColor(builder, color, mixFactor);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(width, height, this.time, 0.0F);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            break;
         case Flow:
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(width, height, this.time, mixFactor);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            break;
         default:
            builder.putVec4(0.0F, 0.0F, 0.0F, mixFactor);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(width, height, this.time, 0.0F);
            builder.putVec4(glowRadius, glowQuality, glowDivider, glowMaxSample);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
            builder.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
      }

      ByteBuffer data = builder.get();
      return RenderSystem.getDevice().createBuffer(() -> "alien_shader_uniforms", 128, data);
   }

   private void putColor(Std140Builder builder, Color color, float w) {
      builder.putVec4(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, w);
   }

   public void reloadShaders() {
      this.shaderBuffer = null;
   }

   public boolean fullNullCheck() {
      if (this.shaderBuffer != null) {
         return false;
      } else if (mc.getMainRenderTarget() == null) {
         return true;
      } else {
         RenderSystem.assertOnRenderThread();
         this.shaderBuffer = new TextureTarget("alien_shader", mc.getMainRenderTarget().width, mc.getMainRenderTarget().height, true);
         return true;
      }
   }

   static {
      for (ShaderManager.Shader shader : ShaderManager.Shader.values()) {
         String name = SHADER_NAMES[shader.ordinal()];
         RenderPipeline pipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/" + name))
            .withVertexShader(Identifier.fromNamespaceAndPath("alienclient", "core/blit"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("alienclient", "core/" + name))
            .withSampler("InSampler")
            .withUniform("AlienParams", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.ENTITY_OUTLINE_BLIT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
            .build();
         PIPELINES[shader.ordinal()] = RenderPipelines.register(pipeline);
      }
   }

   public record RenderTask(Runnable task, ShaderManager.Shader shader) {
   }

   public static enum Shader {
      Solid,
      Pulse,
      Smoke,
      Gradient,
      Snow,
      Flow,
      Rainbow;
   }
}
