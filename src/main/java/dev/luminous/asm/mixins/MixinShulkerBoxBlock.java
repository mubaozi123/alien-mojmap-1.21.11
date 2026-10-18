package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class MixinShulkerBoxBlock {
   @Inject(method = "addDetailsToTooltip", at = @At("HEAD"), cancellable = true)
   private void onAppendTooltip(
      TooltipContext context, TooltipDisplay displayComponent, Player player, TooltipFlag type, Consumer<Component> textConsumer, CallbackInfo ci
   ) {
      if (((ItemStack)(Object)this).getItem() instanceof BlockItem blockItem
         && blockItem.getBlock() instanceof ShulkerBoxBlock
         && ShulkerViewer.INSTANCE.isOn()
         && ShulkerViewer.INSTANCE.toolTips.getValue()) {
         ci.cancel();
      }
   }
}
