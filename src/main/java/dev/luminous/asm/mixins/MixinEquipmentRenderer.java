package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentRenderer {
   @Redirect(
      method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z")
   )
   private boolean redirectArmorGlint(ItemStack itemStack) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorGlint.getValue() ? false : itemStack.hasFoil();
   }

   @Redirect(
      method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
   )
   private Object redirectArmorTrim(ItemStack itemStack, DataComponentType<?> componentType) {
      return componentType == DataComponents.TRIM && NoRender.INSTANCE.isOn() && NoRender.INSTANCE.armorTrim.getValue() ? null : itemStack.get(componentType);
   }
}
