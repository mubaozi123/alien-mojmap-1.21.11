package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void adjustShadow(ItemEntity itemEntity, ItemEntityRenderState renderState, float partialTicks, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fastItem.getValue()) {
         renderState.shadowRadius = NoRender.INSTANCE.castShadow.getValue() ? 0.15F : 0.0F;
      }
   }
}
