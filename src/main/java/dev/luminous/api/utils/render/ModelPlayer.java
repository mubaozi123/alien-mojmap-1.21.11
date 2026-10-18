package dev.luminous.api.utils.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.asm.accessors.IModelPart;
import dev.luminous.asm.accessors.IModelPartCuboid;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModelPlayer extends PlayerModel {
   public final Player player;
   private final PlayerSkin skin;
   private static final Vector4f pos1 = new Vector4f();
   private static final Vector4f pos2 = new Vector4f();
   private static final Vector4f pos3 = new Vector4f();
   private static final Vector4f pos4 = new Vector4f();

   public ModelPlayer(Player player) {
      super(Wrapper.mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      this.player = player;
      this.skin = player instanceof ClientAvatarEntity clientPlayer ? clientPlayer.getSkin() : DefaultPlayerSkin.getDefaultSkin();
      this.leftPants.visible = false;
      this.rightPants.visible = false;
      this.leftSleeve.visible = false;
      this.rightSleeve.visible = false;
      this.jacket.visible = false;
      this.hat.visible = false;
      this.getHead().offsetScale(new Vector3f(-0.05F, -0.05F, -0.05F));
   }

   public void render(PoseStack matrices, ColorSetting fill, ColorSetting line) {
      this.render(matrices, fill, line, 1.0, 0.0, 1.0, 0.0, false, false);
   }

   public void render(
      PoseStack matrices, ColorSetting fill, ColorSetting line, double alpha, double yOffset, double scale, double yaw, boolean noLimb, boolean forceSneaking
   ) {
      AvatarRenderState state = new AvatarRenderState();
      state.bodyRot = this.player.yBodyRot;
      state.xRot = this.player.getXRot();
      state.walkAnimationSpeed = this.player.walkAnimation.speed();
      state.walkAnimationPos = this.player.walkAnimation.position();
      state.isCrouching = this.player.isShiftKeyDown();
      state.isFallFlying = this.player.isFallFlying();
      state.isVisuallySwimming = this.player.isVisuallySwimming();
      state.attackTime = this.player.attackAnim;
      state.swimAmount = ((ILivingEntity)this.player).getLeaningPitch();
      if (forceSneaking) {
         state.isCrouching = true;
      }

      if (noLimb) {
         state.walkAnimationPos = 0.0F;
         state.walkAnimationSpeed = 0.0F;
      }

      double x = this.player.getX() - Wrapper.mc.gameRenderer.getMainCamera().position().x();
      double y = this.player.getY() - Wrapper.mc.gameRenderer.getMainCamera().position().y() + yOffset;
      double z = this.player.getZ() - Wrapper.mc.gameRenderer.getMainCamera().position().z();
      matrices.pushPose();
      matrices.translate((float)x, (float)y, (float)z);
      matrices.mulPose(Axis.YP.rotation(MathUtil.rad(180.0F - this.player.yBodyRot + (float)yaw)));
      float j = ((ILivingEntity)this.player).getLeaningPitch();
      if (this.player.isFallFlying()) {
         float k = this.player.getXRot();
         float l = this.player.getFallFlyingTicks() + this.player.yBodyRot + (float)yaw;
         float m = Mth.clamp(l * l / 100.0F, 0.0F, 1.0F);
         if (!this.player.isAutoSpinAttack()) {
            matrices.mulPose(Axis.XP.rotationDegrees(m * (-90.0F - k)));
         }

         Vec3 vec3d = this.player.getViewVector(1.0F);
         Vec3 vec3d2 = this.player.getDeltaMovement();
         double d = vec3d2.horizontalDistanceSqr();
         double e = vec3d.horizontalDistanceSqr();
         if (d > 0.0 && e > 0.0) {
            double n = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
            double o = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
            matrices.mulPose(Axis.YP.rotation((float)(Math.signum(o) * Math.acos(n))));
         }
      } else if (j > 0.0F) {
         float kx = this.player.getXRot();
         float lx = this.player.isInWater() ? -90.0F - kx : -90.0F;
         float mx = Mth.lerp(j, 0.0F, lx);
         matrices.mulPose(Axis.XP.rotationDegrees(mx));
         if (this.player.isVisuallySwimming()) {
            matrices.translate(0.0F, -1.0F, 0.3F);
         }
      }

      matrices.scale(-1.0F, -1.0F, 1.0F);
      matrices.translate(0.0F, -1.401F, 0.0F);
      matrices.scale((float)scale * 0.93F, (float)scale * 0.93F, (float)scale * 0.93F);
      this.setupAnim(state);
      GlStateManager._enableBlend();
      GlStateManager._disableDepthTest();
      render(matrices, this.head, fill, line, alpha, false);
      render(matrices, this.body, fill, line, alpha, false);
      render(matrices, this.leftArm, fill, line, alpha, false);
      render(matrices, this.rightArm, fill, line, alpha, false);
      render(matrices, this.leftLeg, fill, line, alpha, false);
      render(matrices, this.rightLeg, fill, line, alpha, false);
      matrices.popPose();
      GlStateManager._disableBlend();
      GlStateManager._enableDepthTest();
   }

   public static void render(PoseStack matrices, ModelPart part, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
      if (part.visible && (!((IModelPart)(Object)part).getCuboids().isEmpty() || !((IModelPart)(Object)part).getChildren().isEmpty())) {
         matrices.pushPose();
         part.translateAndRotate(matrices);

         for (Cube cuboid : ((IModelPart)(Object)part).getCuboids()) {
            render(matrices, cuboid, fill, line, alpha, texture);
         }

         for (ModelPart child : ((IModelPart)(Object)part).getChildren().values()) {
            render(matrices, child, fill, line, alpha, texture);
         }

         matrices.popPose();
      }
   }

   public static void render(PoseStack matrices, Cube cuboid, ColorSetting fill, ColorSetting line, double alpha, boolean texture) {
      Matrix4f matrix = matrices.last().pose();

      for (Polygon quad : ((IModelPartCuboid)cuboid).getSides()) {
         pos1.set(quad.vertices()[0].x() / 16.0F, quad.vertices()[0].y() / 16.0F, quad.vertices()[0].z() / 16.0F, 1.0F);
         pos1.mul(matrix);
         pos2.set(quad.vertices()[1].x() / 16.0F, quad.vertices()[1].y() / 16.0F, quad.vertices()[1].z() / 16.0F, 1.0F);
         pos2.mul(matrix);
         pos3.set(quad.vertices()[2].x() / 16.0F, quad.vertices()[2].y() / 16.0F, quad.vertices()[2].z() / 16.0F, 1.0F);
         pos3.mul(matrix);
         pos4.set(quad.vertices()[3].x() / 16.0F, quad.vertices()[3].y() / 16.0F, quad.vertices()[3].z() / 16.0F, 1.0F);
         pos4.mul(matrix);
         if (fill.booleanValue) {
            Color color = fill.getValue();
            float keyCodec = (float)(color.getAlpha() / 255.0F * alpha);
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float elementCodec = color.getBlue() / 255.0F;
            BufferBuilder buffer = Tesselator.getInstance()
               .begin(Mode.QUADS, texture ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(pos1.x, pos1.y, pos1.z).setUv(quad.vertices()[0].u(), quad.vertices()[0].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos2.x, pos2.y, pos2.z).setUv(quad.vertices()[1].u(), quad.vertices()[1].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos2.x, pos2.y, pos2.z).setUv(quad.vertices()[1].u(), quad.vertices()[1].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos3.x, pos3.y, pos3.z).setUv(quad.vertices()[2].u(), quad.vertices()[2].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos3.x, pos3.y, pos3.z).setUv(quad.vertices()[2].u(), quad.vertices()[2].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos4.x, pos4.y, pos4.z).setUv(quad.vertices()[3].u(), quad.vertices()[3].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos1.x, pos1.y, pos1.z).setUv(quad.vertices()[0].u(), quad.vertices()[0].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos1.x, pos1.y, pos1.z).setUv(quad.vertices()[0].u(), quad.vertices()[0].v()).setColor(r, g, elementCodec, keyCodec);
            Render3DUtil.endBuilding(buffer);
         }

         if (line.booleanValue) {
            Color color = line.getValue();
            float keyCodec = (float)(color.getAlpha() / 255.0F * alpha);
            float r = color.getRed() / 255.0F;
            float g = color.getGreen() / 255.0F;
            float elementCodec = color.getBlue() / 255.0F;
            BufferBuilder buffer = Tesselator.getInstance()
               .begin(Mode.DEBUG_LINES, texture ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
            buffer.addVertex(pos1.x, pos1.y, pos1.z).setUv(quad.vertices()[0].u(), quad.vertices()[0].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos2.x, pos2.y, pos2.z).setUv(quad.vertices()[1].u(), quad.vertices()[1].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos2.x, pos2.y, pos2.z).setUv(quad.vertices()[1].u(), quad.vertices()[1].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos3.x, pos3.y, pos3.z).setUv(quad.vertices()[2].u(), quad.vertices()[2].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos3.x, pos3.y, pos3.z).setUv(quad.vertices()[2].u(), quad.vertices()[2].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos4.x, pos4.y, pos4.z).setUv(quad.vertices()[3].u(), quad.vertices()[3].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos4.x, pos4.y, pos4.z).setUv(quad.vertices()[3].u(), quad.vertices()[3].v()).setColor(r, g, elementCodec, keyCodec);
            buffer.addVertex(pos1.x, pos1.y, pos1.z).setUv(quad.vertices()[0].u(), quad.vertices()[0].v()).setColor(r, g, elementCodec, keyCodec);
            Render3DUtil.endBuilding(buffer);
         }
      }
   }

   public void renderPlayerSkin(PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, float tickDelta) {
      if (this.player instanceof AbstractClientPlayer clientPlayer) {
         AvatarRenderer playerRenderer = (AvatarRenderer)Wrapper.mc.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(clientPlayer);
         AvatarRenderState state = playerRenderer.createRenderState();
         playerRenderer.extractRenderState(clientPlayer, state, tickDelta);
         state.skin = this.skin;
         EntityRenderer renderer = Wrapper.mc.getEntityRenderDispatcher().getRenderer(state);
         Vec3 cameraPos = Wrapper.mc.gameRenderer.getMainCamera().position();
         Vec3 offset = renderer.getRenderOffset(state);
         matrices.pushPose();
         matrices.translate(state.x - cameraPos.x + offset.x, state.y - cameraPos.y + offset.y, state.z - cameraPos.z + offset.z);
         renderer.submit(state, matrices, queue, cameraState);
         matrices.popPose();
      }
   }

   public static void renderTexture(PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, Entity entity, float tickDelta) {
      EntityRenderer renderer = Wrapper.mc.getEntityRenderDispatcher().getRenderer(entity);
      EntityRenderState state = renderer.createRenderState();
      renderer.extractRenderState(entity, state, tickDelta);
      Vec3 cameraPos = Wrapper.mc.gameRenderer.getMainCamera().position();
      Vec3 offset = renderer.getRenderOffset(state);
      matrices.pushPose();
      matrices.translate(state.x - cameraPos.x + offset.x, state.y - cameraPos.y + offset.y, state.z - cameraPos.z + offset.z);
      renderer.submit(state, matrices, queue, cameraState);
      matrices.popPose();
   }
}
