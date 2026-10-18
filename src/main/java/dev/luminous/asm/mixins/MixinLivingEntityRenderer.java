package dev.luminous.asm.mixins;

import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private boolean alien$rotating;
   @Unique
   private float alien$originalYaw;
   @Unique
   private float alien$originalHeadYaw;
   @Unique
   private float alien$originalBodyYaw;
   @Unique
   private float alien$originalPitch;
   @Unique
   private float alien$originalLastYaw;
   @Unique
   private float alien$originalLastHeadYaw;
   @Unique
   private float alien$originalLastBodyYaw;
   @Unique
   private float alien$originalLastPitch;

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("HEAD")
   )
   private void alien$beforeUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickProgress, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      this.alien$rotating = mc.player != null && entity == mc.player && ClientSetting.INSTANCE.rotations.getValue();
      if (this.alien$rotating) {
         this.alien$originalYaw = entity.getYRot();
         this.alien$originalHeadYaw = entity.yHeadRot;
         this.alien$originalBodyYaw = entity.yBodyRot;
         this.alien$originalPitch = entity.getXRot();
         this.alien$originalLastYaw = entity.yRotO;
         this.alien$originalLastHeadYaw = entity.yHeadRotO;
         this.alien$originalLastBodyYaw = entity.yBodyRotO;
         this.alien$originalLastPitch = entity.xRotO;
         entity.setYRot(RotationManager.getRenderYawOffset());
         entity.yHeadRot = RotationManager.getRotationYawHead();
         entity.yBodyRot = RotationManager.getRenderYawOffset();
         entity.setXRot(RotationManager.getRenderPitch());
         entity.yRotO = RotationManager.getPrevRenderYawOffset();
         entity.yHeadRotO = RotationManager.getPrevRotationYawHead();
         entity.yBodyRotO = RotationManager.getPrevRenderYawOffset();
         entity.xRotO = RotationManager.getPrevRenderPitch();
      }
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void alien$afterUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickProgress, CallbackInfo ci) {
      if (this.alien$rotating) {
         this.alien$rotating = false;
         entity.setYRot(this.alien$originalYaw);
         entity.yHeadRot = this.alien$originalHeadYaw;
         entity.yBodyRot = this.alien$originalBodyYaw;
         entity.setXRot(this.alien$originalPitch);
         entity.yRotO = this.alien$originalLastYaw;
         entity.yHeadRotO = this.alien$originalLastHeadYaw;
         entity.yBodyRotO = this.alien$originalLastBodyYaw;
         entity.xRotO = this.alien$originalLastPitch;
      }
   }
}
