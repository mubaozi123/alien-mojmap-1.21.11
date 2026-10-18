package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IGameRenderer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogRenderer.FogMode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   final ColorSetting colorConfig = this.add(new ColorSetting("Color", new Color(255, 255, 255)));
   final ColorSetting friendConfig = this.add(new ColorSetting("Friend", new Color(155, 155, 255)).injectBoolean(true));
   final ColorSetting invisibleConfig = this.add(new ColorSetting("Invisible", new Color(200, 200, 200)).injectBoolean(true));
   final ColorSetting died = this.add(new ColorSetting("Died", new Color(180, 0, 0)).injectBoolean(true));
   final ColorSetting sneakingConfig = this.add(new ColorSetting("Sneaking", new Color(200, 200, 0)).injectBoolean(true));
   final ColorSetting rectConfig = this.add(new ColorSetting("Rectangle", new Color(0, 0, 0, 100)).injectBoolean(true));
   final BooleanSetting armorConfig = this.add(new BooleanSetting("Armor", true).setParent());
   final BooleanSetting drawItemConfig = this.add(new BooleanSetting("DrawItem", true, this.armorConfig::isOpen));
   final SliderSetting offsetConfig = this.add(new SliderSetting("Offset", -20.0, -30.0, 10.0, 0.01, this.armorConfig::isOpen));
   final BooleanSetting enchantmentsConfig = this.add(new BooleanSetting("Enchantments", true));
   final BooleanSetting durabilityConfig = this.add(new BooleanSetting("Durability", true).setParent());
   final BooleanSetting forceBarConfig = this.add(new BooleanSetting("ForceBar", true, this.durabilityConfig::isOpen));
   final BooleanSetting itemNameConfig = this.add(new BooleanSetting("ItemName", false));
   final BooleanSetting entityIdConfig = this.add(new BooleanSetting("EntityId", false));
   final BooleanSetting gamemodeConfig = this.add(new BooleanSetting("Gamemode", false));
   final BooleanSetting pingConfig = this.add(new BooleanSetting("Ping", true));
   final BooleanSetting healthConfig = this.add(new BooleanSetting("Health", true));
   final BooleanSetting totemsConfig = this.add(new BooleanSetting("Totems", false));
   final SliderSetting scaleConfig = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.1));
   final BooleanSetting factorConfig = this.add(new BooleanSetting("Factor", true).setParent());
   final SliderSetting scalingConfig = this.add(new SliderSetting("Scaling", 1.0, 0.0, 3.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting distanceConfig = this.add(new SliderSetting("Distance", 6.0, 0.0, 20.0, 0.1, this.factorConfig::isOpen));
   final SliderSetting heightConfig = this.add(new SliderSetting("Height", 0.0, -3.0, 3.0, 0.01));
   final DecimalFormat df = new DecimalFormat("0.0");

   public NameTags() {
      super("NameTags", "Renders info on player NameTags", Module.Category.Render);
      this.setChinese("名字标签");
      INSTANCE = this;
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Camera camera = mc.gameRenderer.getMainCamera();
         ByteBufferBuilder allocator = new ByteBufferBuilder(262144);
         Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> layerBuffers = new Object2ObjectLinkedOpenHashMap();
         List<ByteBufferBuilder> layerAllocators = new ArrayList<>();
         addLayerBuffer(layerBuffers, layerAllocators, RenderTypes.glint());
         addLayerBuffer(layerBuffers, layerAllocators, RenderTypes.glintTranslucent());
         addLayerBuffer(layerBuffers, layerAllocators, RenderTypes.entityGlint());
         addLayerBuffer(layerBuffers, layerAllocators, RenderTypes.armorEntityGlint());
         BufferSource immediate = MultiBufferSource.immediateWithBuffers(layerBuffers, allocator);
         SubmitNodeStorage queue = new SubmitNodeStorage();
         FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
            queue,
            mc.getBlockRenderer(),
            immediate,
            mc.getAtlasManager(),
            mc.renderBuffers().outlineBufferSource(),
            mc.renderBuffers().crumblingBufferSource(),
            mc.font
         );
         boolean renderedAny = false;
         GpuBufferSlice previousFog = RenderSystem.getShaderFog();
         boolean fogDisabled = false;

         try {
            PoseStack matrixStack = new PoseStack();
            matrixStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
            matrixStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));

            for (Player playerEntity : Alien.THREAD.getPlayers()) {
               if ((this.died.booleanValue || playerEntity.isAlive())
                  && (playerEntity != mc.player || !mc.options.getCameraType().isFirstPerson())
                  && (this.invisibleConfig.booleanValue || !playerEntity.isInvisible())) {
                  String info = this.getNametagInfo(playerEntity);
                  Vec3 renderPosition = MathUtil.getRenderPosition(playerEntity, event.tickDelta);
                  double x = renderPosition.x();
                  double y = renderPosition.y();
                  double z = renderPosition.z();
                  int width = mc.font.width(info);
                  this.renderInfo(info, width / 2.0F, playerEntity, x, y, z, camera, matrixStack, queue);
                  renderedAny = true;
               }
            }

            if (renderedAny) {
               if (mc.getMainRenderTarget().getDepthTexture() != null) {
                  RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.getMainRenderTarget().getDepthTexture(), 1.0);
               }

               FogRenderer fogRenderer = ((IGameRenderer)mc.gameRenderer).getFogRenderer();
               RenderSystem.setShaderFog(fogRenderer.getBuffer(FogMode.NONE));
               fogDisabled = true;
               dispatcher.renderAllFeatures();
            }
         } finally {
            immediate.endBatch();
            if (fogDisabled) {
               RenderSystem.setShaderFog(previousFog);
            }

            for (ByteBufferBuilder layerAllocator : layerAllocators) {
               layerAllocator.close();
            }

            allocator.close();
         }
      }
   }

   private static void addLayerBuffer(
      Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> layerBuffers, List<ByteBufferBuilder> allocators, RenderType layer
   ) {
      ByteBufferBuilder layerAllocator = new ByteBufferBuilder(Math.max(layer.bufferSize(), 16384));
      allocators.add(layerAllocator);
      layerBuffers.put(layer, layerAllocator);
   }

   private void renderInfo(String info, float width, Player entity, double x, double y, double z, Camera camera, PoseStack matrices, SubmitNodeCollector queue) {
      Vec3 pos = camera.position();
      double eyeY = y + entity.getBbHeight() + (entity.isShiftKeyDown() ? 0.4F : 0.43F) + this.heightConfig.getValueFloat();
      float scale = (float)(
         -0.025F * this.scaleConfig.getValueFloat()
            + (
               this.factorConfig.getValue() && pos.distanceToSqr(x, eyeY, z) > this.distanceConfig.getValueFloat() * this.distanceConfig.getValueFloat()
                  ? (Math.sqrt(pos.distanceToSqr(x, eyeY, z)) - this.distanceConfig.getValueFloat()) * -0.0025F * this.scalingConfig.getValueFloat()
                  : 0.0
            )
      );
      matrices.pushPose();
      matrices.translate(x - pos.x(), eyeY - pos.y(), z - pos.z());
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.scale(scale, scale, -scale);
      if (this.rectConfig.booleanValue) {
         float paddingX = 2.0F;
         float paddingY = 1.0F;
         Render3DUtil.drawRect(matrices, -width - paddingX, -paddingY, width * 2.0F + paddingX * 2.0F, 10.0F, this.rectConfig.getValue().getRGB());
      }

      this.drawWithShadow(queue, matrices, info, -width, 0.0F, this.getNametagColor(entity));
      if (this.armorConfig.getValue()) {
         this.renderItems(matrices, entity, queue);
      }

      matrices.popPose();
   }

   private void drawWithShadow(SubmitNodeCollector queue, PoseStack matrices, String info, float x, float y, int color) {
      FormattedCharSequence mainText = Component.literal(info).getVisualOrderText();
      int shadowColor = Integer.MIN_VALUE;
      String plainText = info.replaceAll("§.", "");
      FormattedCharSequence shadowText = Component.literal(plainText).getVisualOrderText();
      queue.submitText(matrices, x + 1.0F, y + 1.0F, shadowText, true, DisplayMode.SEE_THROUGH, 15728880, shadowColor, 0, 15728880);
      queue.submitText(matrices, x, y, mainText, false, DisplayMode.SEE_THROUGH, 15728880, color, 0, 15728880);
   }

   private void renderItems(PoseStack matrixStack, Player player, SubmitNodeCollector queue) {
      CopyOnWriteArrayList<ItemStack> displayItems = new CopyOnWriteArrayList<>();
      if (!player.getOffhandItem().isEmpty()) {
         displayItems.add(player.getOffhandItem());
      }

      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
         ItemStack armorStack = player.getItemBySlot(slot);
         if (!armorStack.isEmpty()) {
            displayItems.add(armorStack);
         }
      }

      if (!player.getMainHandItem().isEmpty()) {
         displayItems.add(player.getMainHandItem());
      }

      Collections.reverse(displayItems);
      float x = 0.0F;
      int n11 = 0;

      for (ItemStack itemStack : displayItems) {
         x -= 8.0F;
         if (itemStack.getEnchantments().size() > n11) {
            n11 = itemStack.getEnchantments().size();
         }
      }

      float y = this.offsetConfig.getValueFloat();

      for (ItemStack stack : displayItems) {
         if (this.drawItemConfig.getValue()) {
            this.renderItemStack(matrixStack, stack, x, y + 1.0F, queue);
         }

         this.renderItemOverlay(matrixStack, stack, x, y + 2.5F, queue);
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.durabilityConfig.getValue()) {
            this.renderDurability(matrixStack, stack, x + 2.0F, y + 11.5F, queue);
         }

         if (this.enchantmentsConfig.getValue()) {
            this.renderEnchants(matrixStack, stack, x + 2.0F, y + 7.0F, queue);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
         x += 16.0F;
      }

      ItemStack itemStackx = player.getMainHandItem();
      if (!itemStackx.isEmpty()) {
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         if (this.itemNameConfig.getValue()) {
            this.renderItemName(matrixStack, itemStackx, y - 4.5F + this.enchantOffset(n11), queue);
         }

         matrixStack.scale(2.0F, 2.0F, 2.0F);
      }
   }

   private void renderItemStack(PoseStack matrixStack, ItemStack stack, float x, float y, SubmitNodeCollector queue) {
      matrixStack.pushPose();
      matrixStack.translate(x, y, 0.0F);
      matrixStack.translate(8.0F, 8.0F, 0.0F);
      matrixStack.scale(16.0F, 16.0F, 1.0E-8F);
      matrixStack.mulPose(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
      ItemStackRenderState renderState = new ItemStackRenderState();
      mc.getItemModelResolver().appendItemLayers(renderState, stack, ItemDisplayContext.GUI, mc.level, null, 0);
      renderState.submit(matrixStack, queue, 15728880, OverlayTexture.NO_OVERLAY, 0);
      matrixStack.popPose();
   }

   private void renderItemOverlay(PoseStack matrixStack, ItemStack stack, float x, float y, SubmitNodeCollector queue) {
      matrixStack.pushPose();
      if (stack.getCount() != 1) {
         String string = String.valueOf(stack.getCount());
         this.drawWithShadow(queue, matrixStack, string, x + 17.0F - mc.font.width(string), y + 9.0F, -1);
      }

      if (stack.isBarVisible() || stack.isDamageableItem() && this.forceBarConfig.getValue()) {
         int i = stack.getBarWidth();
         int j = stack.getBarColor();
         float k = x + 2.0F;
         float l = y + 13.0F;
         Render3DUtil.drawRect(matrixStack, k, l, 13.0F, 2.0F, -16777216);
         Render3DUtil.drawRect(matrixStack, k, l, i, 1.0F, j | 0xFF000000);
      }

      matrixStack.popPose();
   }

   private void renderDurability(PoseStack matrixStack, ItemStack itemStack, float x, float y, SubmitNodeCollector queue) {
      if (itemStack.isDamageableItem()) {
         int n = itemStack.getMaxDamage();
         int n2 = itemStack.getDamageValue();
         int durability = (int)((float)(n - n2) / n * 100.0F);
         this.drawWithShadow(
            queue, matrixStack, durability + "%", x * 2.0F, y * 2.0F, ColorUtil.hslToColor((float)(n - n2) / n * 120.0F, 100.0F, 50.0F, 1.0F).getRGB()
         );
      }
   }

   private void renderEnchants(PoseStack matrixStack, ItemStack itemStack, float x, float y, SubmitNodeCollector queue) {
      if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         this.drawWithShadow(queue, matrixStack, "God", x * 2.0F, y * 2.0F, -3977663);
      } else if (itemStack.isEnchanted()) {
         ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
         float n2 = 0.0F;

         for (Holder enchantment : enchants.keySet()) {
            int lvl = enchants.getLevel(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = ((Enchantment)enchantment.value()).toString().replace("Enchantment ", "");
            if (translatedName.contains("Vanish")) {
               enchantString.append("§cVan");
            } else if (translatedName.contains("Bind")) {
               enchantString.append("§cBind");
            } else {
               int maxLen = lvl > 1 ? 2 : 3;
               if (translatedName.length() > maxLen) {
                  translatedName = translatedName.substring(0, maxLen);
               }

               enchantString.append(translatedName);
               enchantString.append(lvl);
            }

            this.drawWithShadow(queue, matrixStack, enchantString.toString(), x * 2.0F, (y + n2) * 2.0F, -1);
            n2 -= 4.5F;
         }
      }
   }

   private float enchantOffset(int n) {
      if (this.enchantmentsConfig.getValue() && n > 2) {
         float value = -2.0F;
         float var3;
         return var3 = value - (n - 3) * 4.5F;
      } else {
         return 0.0F;
      }
   }

   private void renderItemName(PoseStack matrixStack, ItemStack itemStack, float y, SubmitNodeCollector queue) {
      String itemName = itemStack.getHoverName().getString();
      float width = mc.font.width(itemName) / 4.0F;
      this.drawWithShadow(queue, matrixStack, itemName, (0.0F - width) * 2.0F, y * 2.0F, -1);
   }

   private String getNametagInfo(Player player) {
      StringBuilder info = new StringBuilder();
      if (this.gamemodeConfig.getValue()) {
         if (player.isCreative()) {
            info.append(ChatFormatting.GOLD);
            info.append("[C] ");
         } else if (player.isSpectator()) {
            info.append(ChatFormatting.GRAY);
            info.append("[I] ");
         } else {
            info.append(ChatFormatting.BOLD);
            info.append("[S] ");
         }
      }

      if (this.pingConfig.getValue()) {
         info.append(this.getEntityPing(player));
         info.append("ms ");
         info.append(ChatFormatting.RESET);
      }

      info.append(player.getName().getString());
      info.append(" ");
      if (this.entityIdConfig.getValue()) {
         info.append("ID: ");
         info.append(player.getId());
         info.append(" ");
      }

      if (this.healthConfig.getValue()) {
         double health = player.getHealth() + player.getAbsorptionAmount();
         ChatFormatting hcolor = health > 18.0
            ? ChatFormatting.GREEN
            : (
               health > 16.0
                  ? ChatFormatting.DARK_GREEN
                  : (
                     health > 12.0
                        ? ChatFormatting.YELLOW
                        : (health > 8.0 ? ChatFormatting.GOLD : (health > 4.0 ? ChatFormatting.RED : ChatFormatting.DARK_RED))
                  )
            );
         String phealth = this.df.format(health);
         info.append(hcolor);
         info.append(phealth);
         info.append(" ");
      }

      int totems;
      if (this.totemsConfig.getValue() && (totems = Alien.POP.getPop(player)) > 0) {
         ChatFormatting c = TextRadar.getPopColor(totems);
         info.append(c);
         info.append(-totems);
         info.append(" ");
      }

      return info.toString().trim();
   }

   private String getEntityPing(Player entity) {
      if (mc.getConnection() == null) {
         return "§7-1";
      } else {
         PlayerInfo playerListEntry = mc.getConnection().getPlayerInfo(entity.getUUID());
         if (playerListEntry == null) {
            return "§7-1";
         } else {
            int ping = playerListEntry.getLatency();
            ChatFormatting color = ping >= 200 ? ChatFormatting.RED : (ping >= 100 ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
            return color.toString() + ping;
         }
      }
   }

   private int getNametagColor(Player player) {
      if (this.friendConfig.booleanValue && player.getDisplayName() != null && Alien.FRIEND.isFriend(player)) {
         return this.friendConfig.getValue().getRGB();
      } else if (this.invisibleConfig.booleanValue && player.isInvisible()) {
         return this.invisibleConfig.getValue().getRGB();
      } else if (this.sneakingConfig.booleanValue && player.isShiftKeyDown()) {
         return this.sneakingConfig.getValue().getRGB();
      } else {
         return !player.isAlive() ? this.died.getValue().getRGB() : this.colorConfig.getValue().getRGB();
      }
   }
}
