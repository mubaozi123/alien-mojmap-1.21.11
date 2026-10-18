package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.NoRender;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinInGameHud {
   @Final
   @Shadow
   private Minecraft minecraft;
   @Unique
   final Color minColor = new Color(196, 0, 0);
   @Unique
   final Color maxColor = new Color(0, 227, 0);

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderPortalOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.portal.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(CallbackInfo info) {
      if (NoRender.INSTANCE.isOn() && NoRender.INSTANCE.potionsIcon.getValue()) {
         info.cancel();
      }
   }

   @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
   private void onRenderMainHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Player player;
      if (HUD.INSTANCE.isOn() && HUD.INSTANCE.armor.getValue() && (player = this.getCameraPlayer()) != null) {
         int x = context.guiWidth() / 2 + 91;
         int y = context.guiHeight() - 28 - HUD.INSTANCE.armorOffset.getValueInt();
         if (this.minecraft.gameMode.canHurtPlayer()) {
            y -= 16;
         }

         int t;
         if ((t = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth())) == 0) {
            y -= 10;
         }

         int maxAir = player.getMaxAirSupply();
         int air = Math.min(player.getAirSupply(), maxAir);
         if (player.isEyeInFluid(FluidTags.WATER) || air < maxAir) {
            int w = this.getVisibleVehicleHeartRows(t) - 1;
            y += w * 10;
         }

         for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = this.minecraft.player.getItemBySlot(slot);
            x -= 20;
            if (!armor.isEmpty()) {
               int damage = EntityUtil.getDamagePercent(armor);
               context.renderItem(armor, x, y);
               context.renderItemDecorations(this.minecraft.font, armor, x, y);
               if (HUD.INSTANCE.durability.getValue()) {
                  if (HUD.INSTANCE.font.getValue()) {
                     FontManager.small
                        .drawString(
                           context.pose(),
                           damage + "%",
                           x + 1,
                           y - FontManager.small.getFontHeight() / 2.0F,
                           ColorUtil.fadeColor(this.minColor, this.maxColor, damage / 100.0F).getRGB(),
                           HUD.INSTANCE.shadow.getValue()
                        );
                  } else {
                     String string = damage + "%";
                     float f = x + 2;
                     float f2 = y;
                     Objects.requireNonNull(this.minecraft.font);
                     TextUtil.drawStringScale(
                        context,
                        string,
                        f,
                        f2 - 2.25F,
                        ColorUtil.fadeColor(this.minColor, this.maxColor, damage / 100.0F).getRGB(),
                        0.5F,
                        HUD.INSTANCE.shadow.getValue()
                     );
                  }
               }
            }
         }
      }
   }

   @Shadow
   private int getVisibleVehicleHeartRows(int t) {
      return 0;
   }

   @Shadow
   private int getVehicleMaxHearts(LivingEntity livingEntity) {
      return 0;
   }

   @Shadow
   private Player getCameraPlayer() {
      return null;
   }

   @Shadow
   private LivingEntity getPlayerVehicleWithHealth() {
      return null;
   }

   @Shadow
   public abstract void render(GuiGraphics var1, DeltaTracker var2);

   @Inject(at = @At("HEAD"), method = "render")
   public void renderStart(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Alien.EVENT_BUS.post(PreRender2DEvent.get(context));
   }

   @Inject(at = @At("TAIL"), method = "render")
   public void renderHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      Alien.MODULE.onRender2D(context);
   }

   @Inject(
      method = "onDisconnected",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"),
      cancellable = true
   )
   private void onClear(CallbackInfo info) {
      if (ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.keepHistory.getValue()) {
         info.cancel();
      }
   }

   @ModifyArg(
      method = "renderItemHotbar",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
         ordinal = 1
      ),
      index = 2
   )
   private int selectedSlotX(int x) {
      if (ClientSetting.INSTANCE.hotbar()) {
         double hotbarX = ClientSetting.animation.get(x, ClientSetting.INSTANCE.hotbarTime.getValueInt(), ClientSetting.INSTANCE.animEase.getValue());
         return (int)hotbarX;
      } else {
         return x;
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshairBegin(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (Crosshair.INSTANCE.isOn()) {
         Crosshair.INSTANCE.draw(context);
         ci.cancel();
      }
   }
}
