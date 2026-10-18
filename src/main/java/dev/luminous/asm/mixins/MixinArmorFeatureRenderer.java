package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {
   @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
   private void onRenderArmor(PoseStack matrices, SubmitNodeCollector queue, ItemStack stack, EquipmentSlot slot, int light, S state, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorParts.getValue()) {
         ci.cancel();
      }
   }
}
