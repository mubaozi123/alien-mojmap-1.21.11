package dev.luminous.api.utils.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ImmediateDrawUtil {
   private static final CachedOrthoProjectionMatrixBuffer GUI_PROJECTION_MATRIX = new CachedOrthoProjectionMatrixBuffer("alien_gui", 1000.0F, 11000.0F, true);
   private static final RenderPipeline POS_COLOR_QUADS = registerPosColor("quads", Mode.QUADS);
   private static final RenderPipeline POS_COLOR_LINES = registerPosColor("lines", Mode.LINES);
   private static final RenderPipeline POS_COLOR_DEBUG_LINES = registerPosColor("debug_lines", Mode.DEBUG_LINES);
   private static final RenderPipeline POS_COLOR_DEBUG_LINE_STRIP = registerPosColor("debug_line_strip", Mode.DEBUG_LINE_STRIP);
   private static final RenderPipeline POS_COLOR_TRIANGLE_FAN = registerPosColor("triangle_fan", Mode.TRIANGLE_FAN);
   private static final RenderPipeline POS_COLOR_TRIANGLE_STRIP = registerPosColor("triangle_strip", Mode.TRIANGLE_STRIP);
   private static final RenderPipeline LINES_WIDE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/lines_wide"))
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .build()
   );
   private static final RenderPipeline POS_TEX_COLOR_QUADS = registerPosTexColor("quads", Mode.QUADS);
   private static final RenderPipeline POS_TEX_COLOR_DEBUG_LINES = registerPosTexColor("debug_lines", Mode.DEBUG_LINES);
   private static final RenderPipeline POS_COLOR_QUADS_NO_DEPTH = registerPosColorNoDepth("quads", Mode.QUADS);
   private static final RenderPipeline POS_COLOR_LINES_NO_DEPTH = registerPosColorNoDepth("lines", Mode.LINES);
   private static final RenderPipeline POS_COLOR_DEBUG_LINES_NO_DEPTH = registerPosColorNoDepth("debug_lines", Mode.DEBUG_LINES);
   private static final RenderPipeline POS_COLOR_DEBUG_LINE_STRIP_NO_DEPTH = registerPosColorNoDepth("debug_line_strip", Mode.DEBUG_LINE_STRIP);
   private static final RenderPipeline POS_COLOR_TRIANGLE_FAN_NO_DEPTH = registerPosColorNoDepth("triangle_fan", Mode.TRIANGLE_FAN);
   private static final RenderPipeline POS_COLOR_TRIANGLE_STRIP_NO_DEPTH = registerPosColorNoDepth("triangle_strip", Mode.TRIANGLE_STRIP);
   private static final RenderPipeline LINES_WIDE_NO_DEPTH = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/lines_wide_no_depth"))
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .build()
   );

   private ImmediateDrawUtil() {
   }

   private static RenderPipeline registerPosColor(String name, Mode drawMode) {
      return RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/pos_color_" + name))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, drawMode)
            .build()
      );
   }

   private static RenderPipeline registerPosColorNoDepth(String name, Mode drawMode) {
      return RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/pos_color_no_depth_" + name))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, drawMode)
            .build()
      );
   }

   private static RenderPipeline registerPosTexColor(String name, Mode drawMode) {
      return RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("alienclient", "pipeline/pos_tex_color_" + name))
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, drawMode)
            .build()
      );
   }

   public static void drawGui(BufferBuilder builder) {
      drawGui(builder, true);
   }

   public static void drawGui(BufferBuilder builder, boolean noDepth) {
      drawGui(builder, null, noDepth);
   }

   public static void drawGuiTextured(BufferBuilder builder, GpuTextureView textureView) {
      drawGui(builder, textureView, false);
   }

   public static void drawGuiTextured(BufferBuilder builder, GpuTextureView textureView, boolean noDepth) {
      drawGui(builder, textureView, noDepth);
   }

   public static void drawGuiNoDepth(BufferBuilder builder) {
      drawGui(builder, true);
   }

   public static void drawWorld(BufferBuilder builder) {
      drawWorld(builder, false);
   }

   public static void drawWorld(BufferBuilder builder, boolean noDepth) {
      drawWorld(builder, null, noDepth);
   }

   public static void drawWorldTextured(BufferBuilder builder, GpuTextureView textureView) {
      drawWorld(builder, textureView, false);
   }

   public static void drawWorld(BufferBuilder builder, GpuTextureView textureView, boolean noDepth) {
      draw(builder, new Matrix4f(), textureView, noDepth);
   }

   private static void drawGui(BufferBuilder builder, GpuTextureView textureView, boolean noDepth) {
      Minecraft client = Minecraft.getInstance();
      Window window = client.getWindow();
      RenderSystem.backupProjectionMatrix();

      try {
         RenderSystem.setProjectionMatrix(
            GUI_PROJECTION_MATRIX.getBuffer((float)window.getWidth() / window.getGuiScale(), (float)window.getHeight() / window.getGuiScale()),
            ProjectionType.ORTHOGRAPHIC
         );
         draw(builder, new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F), textureView, noDepth);
      } finally {
         RenderSystem.restoreProjectionMatrix();
      }
   }

   private static void draw(BufferBuilder builder, Matrix4f modelView, GpuTextureView textureView, boolean noDepth) {
      try (MeshData builtBuffer = builder.buildOrThrow()) {
         DrawState parameters = builtBuffer.drawState();
         RenderPipeline pipeline = pipelineFor(parameters.mode(), parameters.format(), noDepth);
         RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();
         AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(parameters.mode());
         GpuBuffer indexBuffer = shapeIndexBuffer.getBuffer(parameters.indexCount());
         IndexType indexType = shapeIndexBuffer.type();
         GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
         GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(builtBuffer.vertexBuffer());
         GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
            ? RenderSystem.outputColorTextureOverride
            : framebuffer.getColorTextureView();
         GpuTextureView depthView;
         if (noDepth) {
            depthView = null;
         } else {
            depthView = framebuffer.useDepth
               ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthTextureView())
               : null;
         }

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "alien_immediate", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setPipeline(pipeline);
            if (textureView != null) {
               renderPass.bindTexture("Sampler0", textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            }

            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, indexType);
            renderPass.drawIndexed(0, 0, parameters.indexCount(), 1);
         }
      }
   }

   private static RenderPipeline pipelineFor(Mode drawMode, VertexFormat format, boolean noDepth) {
      if (noDepth) {
         if (format == DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH) {
            return LINES_WIDE_NO_DEPTH;
         } else {
            return switch (drawMode) {
               case QUADS -> POS_COLOR_QUADS_NO_DEPTH;
               case LINES -> POS_COLOR_LINES_NO_DEPTH;
               case DEBUG_LINES -> POS_COLOR_DEBUG_LINES_NO_DEPTH;
               case DEBUG_LINE_STRIP -> POS_COLOR_DEBUG_LINE_STRIP_NO_DEPTH;
               case TRIANGLE_FAN -> POS_COLOR_TRIANGLE_FAN_NO_DEPTH;
               case TRIANGLE_STRIP -> POS_COLOR_TRIANGLE_STRIP_NO_DEPTH;
               default -> throw new IllegalArgumentException("Unsupported no-depth draw mode: " + drawMode);
            };
         }
      } else if (format == DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH) {
         return LINES_WIDE;
      } else if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
         return switch (drawMode) {
            case QUADS -> POS_TEX_COLOR_QUADS;
            case DEBUG_LINES -> POS_TEX_COLOR_DEBUG_LINES;
            default -> throw new IllegalArgumentException("Unsupported textured draw mode: " + drawMode);
         };
      } else {
         return switch (drawMode) {
            case QUADS -> POS_COLOR_QUADS;
            case LINES -> POS_COLOR_LINES;
            case DEBUG_LINES -> POS_COLOR_DEBUG_LINES;
            case DEBUG_LINE_STRIP -> POS_COLOR_DEBUG_LINE_STRIP;
            case TRIANGLE_FAN -> POS_COLOR_TRIANGLE_FAN;
            case TRIANGLE_STRIP -> POS_COLOR_TRIANGLE_STRIP;
            default -> throw new IllegalArgumentException("Unsupported draw mode: " + drawMode);
         };
      }
   }
}
