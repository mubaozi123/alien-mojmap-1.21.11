package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Trajectories extends Module {
   static PoseStack matrixStack;
   private final ColorSetting hand = this.add(new ColorSetting("Hand", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting pearl = this.add(new ColorSetting("Pearl", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting arrow = this.add(new ColorSetting("Arrow", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting xp = this.add(new ColorSetting("XP", new Color(255, 255, 255, 255)).injectBoolean(true));

   public Trajectories() {
      super("Trajectories", Module.Category.Render);
      this.setChinese("抛物线预测");
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (!nullCheck()) {
         Trajectories.matrixStack = matrixStack;
         if (this.pearl.booleanValue || this.arrow.booleanValue || this.xp.booleanValue) {
            GlStateManager._disableDepthTest();

            for (Entity en : Alien.THREAD.getEntities()) {
               if (en instanceof ThrownEnderpearl && this.pearl.booleanValue) {
                  this.calcTrajectory(en, this.pearl.getValue());
               }

               if (en instanceof Arrow && this.arrow.booleanValue) {
                  this.calcTrajectory(en, this.arrow.getValue());
               }

               if (en instanceof ThrownExperienceBottle && this.xp.booleanValue) {
                  this.calcTrajectory(en, this.xp.getValue());
               }
            }

            GlStateManager._enableDepthTest();
         }

         if (this.hand.booleanValue) {
            if (!mc.options.getCameraType().isFirstPerson()) {
               return;
            }

            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offHand = mc.player.getOffhandItem();
            InteractionHand hand;
            if (!(mainHand.getItem() instanceof BowItem)
               && !(mainHand.getItem() instanceof CrossbowItem)
               && !this.isThrowable(mainHand.getItem())
               && !AutoPearl.INSTANCE.isOn()) {
               if (!(offHand.getItem() instanceof BowItem) && !(offHand.getItem() instanceof CrossbowItem) && !this.isThrowable(offHand.getItem())) {
                  return;
               }

               hand = InteractionHand.OFF_HAND;
            } else {
               hand = InteractionHand.MAIN_HAND;
            }

            GlStateManager._disableDepthTest();
            boolean prev_bob = mc.options.bobView().get();
            mc.options.bobView().set(false);
            double x = MathUtil.interpolate(mc.player.xOld, mc.player.getX(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            double y = MathUtil.interpolate(mc.player.yOld, mc.player.getY(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            double z = MathUtil.interpolate(mc.player.zOld, mc.player.getZ(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            if (offHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getItemEnchantmentLevel(
                        mc.level.registryAccess().lookupOrThrow(Enchantments.MULTISHOT.registryKey()).getOrThrow(Enchantments.MULTISHOT), offHand
                     )
                     != 0
               || mainHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getItemEnchantmentLevel(
                        mc.level.registryAccess().lookupOrThrow(Enchantments.MULTISHOT.registryKey()).getOrThrow(Enchantments.MULTISHOT), mainHand
                     )
                     != 0) {
               this.calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot() - 10.0F, x, y, z);
               this.calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot(), x, y, z);
               this.calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot() + 10.0F, x, y, z);
            } else {
               this.calcTrajectory(hand == InteractionHand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYRot(), x, y, z);
            }

            mc.options.bobView().set(prev_bob);
            GlStateManager._enableDepthTest();
         }
      }
   }

   private void calcTrajectory(Entity e, Color color) {
      double motionX = e.getDeltaMovement().x;
      double motionY = e.getDeltaMovement().y;
      double motionZ = e.getDeltaMovement().z;
      if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
         double x = e.getX();
         double y = e.getY();
         double z = e.getZ();

         for (int i = 0; i < 300; i++) {
            Vec3 lastPos = new Vec3(x, y, z);
            if (mc.level.getBlockState(new BlockPos((int)(x += motionX), (int)(y += motionY), (int)(z += motionZ))).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            double var20;
            double var21;
            motionY = e instanceof Arrow ? (var20 = motionY - 0.05F) : (var21 = motionY - 0.03F);
            Vec3 pos = new Vec3(x, y, z);
            if (mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player)) != null
                  && (
                     mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player)).getType() == Type.ENTITY
                        || mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player)).getType() == Type.BLOCK
                  )
               || y <= -65.0) {
               break;
            }

            int alpha = (int)MathUtil.clamp(255.0F * ((i + 1) / 10.0F), 0.0F, 255.0F);
            Render3DUtil.drawLine(lastPos, pos, ColorUtil.injectAlpha(color, alpha));
         }
      }
   }

   private void calcTrajectory(Item item, float yaw, double x, double y, double z) {
      y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      if (item == mc.player.getMainHandItem().getItem()) {
         x -= Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z -= Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      } else {
         x += Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
         z += Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
      }

      float maxDist = this.getDistance(item);
      double motionX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(mc.player.getXRot() / 180.0F * (float) Math.PI) * maxDist;
      double motionY = -Mth.sin((mc.player.getXRot() - this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist;
      double motionZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(mc.player.getXRot() / 180.0F * (float) Math.PI) * maxDist;
      float power = mc.player.getTicksUsingItem() / 20.0F;
      power = (power * power + power * 2.0F) / 3.0F;
      if (power > 1.0F) {
         power = 1.0F;
      }

      float getDistance = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= getDistance;
      motionY /= getDistance;
      motionZ /= getDistance;
      float pow = (item instanceof BowItem ? power * 2.0F : (item instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getThrowVelocity(item);
      motionX *= pow;
      motionY *= pow;
      motionZ *= pow;
      motionX += mc.player.getDeltaMovement().x();
      motionY += mc.player.getDeltaMovement().y();
      motionZ += mc.player.getDeltaMovement().z();

      for (int i = 0; i < 300; i++) {
         Vec3 lastPos = new Vec3(x, y, z);
         if (mc.level.getBlockState(new BlockPos((int)(x += motionX), (int)(y += motionY), (int)(z += motionZ))).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         double var35;
         double var36;
         double var37;
         motionY = item instanceof BowItem
            ? (var35 = motionY - 0.05F)
            : (mc.player.getMainHandItem().getItem() instanceof CrossbowItem ? (var36 = motionY - 0.05F) : (var37 = motionY - 0.03F));
         Vec3 pos = new Vec3(x, y, z);

         for (Entity ent : Alien.THREAD.getEntities()) {
            if (!(ent instanceof Arrow)
               && !ent.equals(mc.player)
               && ent.getBoundingBox().intersects(new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
               Render3DUtil.drawBox(matrixStack, ent.getBoundingBox(), this.hand.getValue());
               break;
            }
         }

         BlockHitResult bhr;
         if ((bhr = mc.level.clip(new ClipContext(lastPos, pos, Block.OUTLINE, Fluid.NONE, mc.player))) != null && bhr.getType() == Type.BLOCK) {
            Render3DUtil.drawBox(matrixStack, new AABB(bhr.getBlockPos()), this.hand.getValue());
            break;
         }

         if (y <= -65.0) {
            break;
         }

         if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
            Render3DUtil.drawLine(lastPos, pos, this.hand.getValue());
         }
      }
   }

   private boolean isThrowable(Item item) {
      return item instanceof EnderpearlItem
         || item instanceof TridentItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private float getDistance(Item item) {
      return item instanceof BowItem ? 1.0F : 0.4F;
   }

   private float getThrowVelocity(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.5F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.59F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private int getThrowPitch(Item item) {
      return !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem) && !(item instanceof ExperienceBottleItem) ? 0 : 20;
   }
}
