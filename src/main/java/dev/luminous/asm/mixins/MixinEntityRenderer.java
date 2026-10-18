package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void updateRenderState(Entity entity, EntityRenderState entityRenderState, float partialTicks, CallbackInfo ci) {
      if (NameTags.INSTANCE.isOn()) {
         entityRenderState.nameTag = null;
      }
   }

   @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
   private void hasLabel(Entity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
      if (NameTags.INSTANCE.isOn()) {
         cir.setReturnValue(false);
      }
   }
}
