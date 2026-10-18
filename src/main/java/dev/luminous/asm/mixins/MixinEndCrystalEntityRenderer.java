package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.impl.render.Chams;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public abstract class MixinEndCrystalEntityRenderer extends EntityRenderer<EndCrystal, EndCrystalRenderState> {
   @Shadow
   @Final
   private static Identifier END_CRYSTAL_LOCATION;
   @Unique
   private static final Identifier BLANK = Identifier.withDefaultNamespace("textures/blank.png");
   @Unique
   private static final RenderType END_CRYSTAL_BLANK = RenderTypes.entityCutoutNoCull(BLANK);
   @Unique
   private static final RenderType END_CRYSTAL_CUSTOM = RenderTypes.entityCutoutNoCull(END_CRYSTAL_LOCATION);
   @Unique
   private static final float SINE_45_DEGREES = Mth.sin((float) (Math.PI / 4));
   @Unique
   private static final float PI3 = (float) (Math.PI / 3);
   @Unique
   private static final ModelPart OUTER_PART;
   @Unique
   private static final ModelPart INNER_PART;
   @Unique
   private static final ModelPart CORE_PART;
   @Shadow
   @Final
   private EndCrystalModel model;

   protected MixinEndCrystalEntityRenderer(Context ctx) {
      super(ctx);
   }

   @Unique
   private float yOffset(float age, Chams module) {
      float f = age * module.floatValue.getValueFloat();
      float g = Mth.sin(f * 0.2F) / 2.0F + 0.5F;
      g = (g * g + g) * 0.4F * module.bounceHeight.getValueFloat();
      return g - 1.4F + module.floatOffset.getValueFloat();
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   public void render(EndCrystalRenderState state, PoseStack matrixStack, SubmitNodeCollector queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
      Chams module = Chams.INSTANCE;
      if (module.customCrystal()) {
         ci.cancel();
         float age = module.spinSync.getValue() ? module.age : state.ageInTicks;
         float h = this.yOffset(age, module);
         float j = age * 3.0F * module.spinValue.getValueFloat();
         matrixStack.pushPose();
         matrixStack.scale(2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat(), 2.0F * module.scale.getValueFloat());
         matrixStack.translate(0.0F, -0.5F, 0.0F);
         matrixStack.translate(0.0F, 1.5F + h / 2.0F, 0.0F);
         Quaternionf diagonalQuat = new Quaternionf().setAngleAxis((float) (Math.PI / 3), SINE_45_DEGREES, 0.0F, SINE_45_DEGREES);
         Quaternionf yRotQuat = Axis.YP.rotationDegrees(j);
         Quaternionf outerQuat = yRotQuat.mul(diagonalQuat);
         Quaternionf innerCoreQuat = diagonalQuat.mul(yRotQuat);
         if (module.custom.getValue()) {
            if (module.outerFrame.booleanValue) {
               matrixStack.pushPose();
               matrixStack.mulPose(outerQuat);
               ModelPlayer.render(matrixStack, OUTER_PART, module.fill, module.line, 1.0, module.chamsTexture.getValue());
               matrixStack.popPose();
            }

            if (module.innerFrame.booleanValue) {
               matrixStack.pushPose();
               matrixStack.mulPose(innerCoreQuat);
               ModelPlayer.render(matrixStack, INNER_PART, module.fill, module.line, 1.0, module.chamsTexture.getValue());
               matrixStack.popPose();
            }

            if (module.core.booleanValue) {
               matrixStack.pushPose();
               matrixStack.mulPose(innerCoreQuat);
               ModelPlayer.render(matrixStack, CORE_PART, module.fill, module.line, 1.0, module.chamsTexture.getValue());
               matrixStack.popPose();
            }
         }

         RenderType layer = module.texture.getValue() ? END_CRYSTAL_CUSTOM : END_CRYSTAL_BLANK;
         int k = OverlayTexture.NO_OVERLAY;
         if (state.showsBottom) {
            matrixStack.pushPose();
            queue.submitModelPart(this.model.base, matrixStack, layer, state.lightCoords, k, null);
            matrixStack.popPose();
         }

         if (module.outerFrame.booleanValue) {
            matrixStack.pushPose();
            matrixStack.mulPose(outerQuat);
            int c = module.outerFrame.getValue().getRGB();
            queue.submitModelPart(OUTER_PART, matrixStack, layer, state.lightCoords, k, null, c, null);
            matrixStack.popPose();
         }

         if (module.innerFrame.booleanValue) {
            matrixStack.pushPose();
            matrixStack.mulPose(innerCoreQuat);
            int c = module.innerFrame.getValue().getRGB();
            queue.submitModelPart(INNER_PART, matrixStack, layer, state.lightCoords, k, null, c, null);
            matrixStack.popPose();
         }

         if (module.core.booleanValue) {
            matrixStack.pushPose();
            matrixStack.mulPose(innerCoreQuat);
            int c = module.core.getValue().getRGB();
            queue.submitModelPart(CORE_PART, matrixStack, layer, state.lightCoords, k, null, c, null);
            matrixStack.popPose();
         }

         matrixStack.popPose();
         Vec3 beamOffset = state.beamOffset;
         if (beamOffset != null) {
            matrixStack.pushPose();
            float bx = (float)(-beamOffset.x);
            float by = (float)(-beamOffset.y + h);
            float bz = (float)(-beamOffset.z);
            matrixStack.translate(beamOffset);
            EnderDragonRenderer.submitCrystalBeams(bx, by, bz, age, matrixStack, queue, state.lightCoords);
            matrixStack.popPose();
         }

         super.submit(state, matrixStack, queue, cameraRenderState);
      }
   }

   static {
      MeshDefinition outerData = new MeshDefinition();
      outerData.getRoot().addOrReplaceChild("outer", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      OUTER_PART = LayerDefinition.create(outerData, 64, 32).bakeRoot();
      MeshDefinition innerData = new MeshDefinition();
      innerData.getRoot()
         .addOrReplaceChild("inner", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO.withScale(0.875F));
      INNER_PART = LayerDefinition.create(innerData, 64, 32).bakeRoot();
      MeshDefinition coreData = new MeshDefinition();
      coreData.getRoot()
         .addOrReplaceChild("core", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO.withScale(0.765625F));
      CORE_PART = LayerDefinition.create(coreData, 64, 32).bakeRoot();
   }
}
