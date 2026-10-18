package dev.luminous.asm.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.luminous.Alien;
import dev.luminous.api.utils.render.EntityOverlayRenderer;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.render.AspectRatio;
import dev.luminous.mod.modules.impl.render.Fov;
import dev.luminous.mod.modules.impl.render.HighLight;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
   @Shadow
   @Final
   Minecraft minecraft;
   @Shadow
   private float fovModifier;
   @Shadow
   private float oldFovModifier;
   private boolean renderingPanorama;
   @Unique
   private Entity cameraEntity;
   @Unique
   private float originalYaw;
   @Unique
   private float originalPitch;

   @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
   private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
      if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.totem.getValue()) {
         info.cancel();
      }
   }

   @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), require = 0)
   private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.nausea.getValue() ? 0.0F : Mth.lerp(delta, first, second);
   }

   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(PoseStack matrices, float tickProgress, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.hurtCam.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
   public void hookOutline(CallbackInfoReturnable<Boolean> cir) {
      if (HighLight.INSTANCE.isOn()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "hand", shift = Shift.AFTER),
      method = "renderLevel"
   )
   void render3dHook(DeltaTracker tickCounter, CallbackInfo ci) {
      if (!Module.nullCheck()) {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         PoseStack matrixStack = new PoseStack();
         matrixStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         matrixStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
         TextUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
         TextUtil.lastWorldSpaceMatrix.set(matrixStack.last().pose());
         Alien.FPS.record();
         EntityOverlayRenderer.flushThroughWall();
         Alien.MODULE.render3D(matrixStack);
      }
   }

   @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("HEAD"), cancellable = true)
   public void getFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> ci) {
      if (!this.renderingPanorama) {
         if (Fov.INSTANCE.isOn() || Zoom.on) {
            boolean usingItem = camera.entity() instanceof LivingEntity living && living.isUsingItem();
            if (Fov.INSTANCE.isOn()) {
               float fov = usingItem ? Fov.INSTANCE.itemFov.getValueFloat() : Fov.INSTANCE.fov.getValueFloat();
               if (Zoom.on) {
                  fov = Math.min(Math.max(fov - Zoom.INSTANCE.currentFov, 1.0F), 177.0F);
               }

               ci.setReturnValue(fov);
            } else {
               float d = 70.0F;
               if (changingFov) {
                  d = this.minecraft.options.fov().get().floatValue();
                  d *= Mth.lerp(tickProgress, this.oldFovModifier, this.fovModifier);
               }

               if (Zoom.on) {
                  d = Math.min(Math.max(d - Zoom.INSTANCE.currentFov, 1.0F), 177.0F);
               }

               ci.setReturnValue(d);
            }
         }
      }
   }

   @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
   private static void getNightVisionStrengthHook(LivingEntity entity, float tickProgress, CallbackInfoReturnable<Float> cir) {
      MobEffectInstance statusEffectInstance = entity.getEffect(MobEffects.NIGHT_VISION);
      if (statusEffectInstance == null) {
         cir.setReturnValue(0.0F);
      }
   }

   @Inject(
      method = "renderLevel",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V", shift = Shift.AFTER)
   )
   public void postRender3dHook(DeltaTracker tickCounter, CallbackInfo ci) {
      Alien.SHADER.renderShaders();
   }

   @Inject(method = "getProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("TAIL"), cancellable = true)
   public void getBasicProjectionMatrixHook(float fov, CallbackInfoReturnable<Matrix4f> cir) {
      if (AspectRatio.INSTANCE.isOn()) {
         PoseStack matrixStack = new PoseStack();
         matrixStack.last().pose().identity();
         if (Zoom.on) {
            matrixStack.translate(Zoom.INSTANCE.zoomX, -Zoom.INSTANCE.zoomY, 0.0F);
            matrixStack.scale(Zoom.INSTANCE.zoom, Zoom.INSTANCE.zoom, 1.0F);
         }

         float farPlane = Math.max(
            Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0F, Minecraft.getInstance().options.cloudRange().get().floatValue() * 16.0F
         );
         matrixStack.last()
            .pose()
            .mul(new Matrix4f().setPerspective((float)(fov * Math.PI / 180.0), AspectRatio.INSTANCE.ratio.getValueFloat(), 0.05F, farPlane));
         cir.setReturnValue(matrixStack.last().pose());
      }
   }
}
