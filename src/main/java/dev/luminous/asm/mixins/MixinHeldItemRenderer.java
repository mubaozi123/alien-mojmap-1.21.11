package dev.luminous.asm.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.HeldItemRendererEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
   @Inject(method = "renderArmWithItem", at = @At("HEAD"))
   private void onRenderItem(
      AbstractClientPlayer player,
      float tickProgress,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector orderedRenderCommandQueue,
      int light,
      CallbackInfo ci
   ) {
      HeldItemRendererEvent event = HeldItemRendererEvent.get(hand, item, equipProgress, matrices);
      Alien.EVENT_BUS.post(event);
   }
}
