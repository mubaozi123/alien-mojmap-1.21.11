package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemClusterRenderState.class)
public abstract class MixinItemStackEntityRenderState {
   @Shadow
   public int count;

   @Inject(method = "extractItemGroupRenderState", at = @At("TAIL"))
   private void modifyRenderAmount(Entity entity, ItemStack stack, ItemModelResolver itemModelManager, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.fastItem.getValue()) {
         int count = stack.getCount();
         if (count <= 1) {
            this.count = 1;
         } else if (count <= 16) {
            this.count = 2;
         } else if (count <= 32) {
            this.count = 3;
         } else {
            this.count = count <= 48 ? 4 : 5;
         }
      }
   }
}
