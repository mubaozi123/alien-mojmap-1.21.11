package dev.luminous.asm.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer {
   @Inject(method = "renderLevel", at = @At("TAIL"))
   public void render3dHook(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f basicProjectionMatrix,
      Matrix4f projectionMatrix,
      GpuBufferSlice fogBuffer,
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      if (Alien.MODULE != null) {
         PoseStack matrices = new PoseStack();
         matrices.mulPose(positionMatrix);
         Alien.MODULE.render3DQueue(matrices);
      }
   }
}
