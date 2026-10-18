package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class MixinDrawContext {
   @Unique
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Final
   @Shadow
   private Matrix3x2fStack pose;

   @Inject(method = "renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
   public void onDrawItem(Font textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE.isOn()
         && ShulkerViewer.INSTANCE.icon.getValue()
         && stack.getItem() instanceof BlockItem
         && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
         ShulkerViewer.getItemsInContainerItem(stack, ITEMS);

         for (ItemStack itemStack : ITEMS) {
            if (!itemStack.isEmpty()) {
               Matrix3x2fStack matrixStack = this.pose;
               matrixStack.pushMatrix();
               matrixStack.scale(0.5F, 0.5F);
               this.renderItem(itemStack, x * 2 + 20, y * 2 + 20);
               matrixStack.popMatrix();
               return;
            }
         }
      }
   }

   @Shadow
   public void renderItem(ItemStack item, int x, int y) {
   }
}
