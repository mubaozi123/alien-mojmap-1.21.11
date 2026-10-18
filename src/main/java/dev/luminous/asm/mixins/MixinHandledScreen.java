package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
   @Unique
   private static final ItemStack[] ITEMS = new ItemStack[27];
   @Shadow
   @Nullable
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;

   protected MixinHandledScreen(Component title) {
      super(title);
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      ItemStack itemStack;
      if (click.button() == 2
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && Wrapper.mc.player.containerMenu.getCarried().isEmpty()
         && ShulkerViewer.INSTANCE.isOn()
         && (ShulkerViewer.hasItems(itemStack = this.hoveredSlot.getItem()) || itemStack.getItem() == Items.ENDER_CHEST && Alien.PLAYER.known)) {
         cir.setReturnValue(ShulkerViewer.openContainer(this.hoveredSlot.getItem(), ITEMS, false));
      }
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (ShulkerViewer.INSTANCE.isOn()
         && ShulkerViewer.INSTANCE.toolTips.getValue()
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && this.minecraft.player.inventoryMenu.getCarried().isEmpty()
         && (ShulkerViewer.hasItems(this.hoveredSlot.getItem()) || this.hoveredSlot.getItem().getItem() == Items.ENDER_CHEST && Alien.PLAYER.known)) {
         ShulkerViewer.renderShulkerToolTip(context, mouseX, mouseY, this.hoveredSlot.getItem());
      }
   }

   @Shadow
   @Override
   public abstract void renderBackground(GuiGraphics var1, int var2, int var3, float var4);
}
