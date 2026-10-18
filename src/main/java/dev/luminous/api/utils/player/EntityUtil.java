package dev.luminous.api.utils.player;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.PeekScreen;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import java.lang.reflect.Field;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EntityUtil implements Wrapper {
   public static boolean inInventory() {
      return mc.screen == null
         || mc.screen instanceof OptionsSubScreen
         || mc.screen instanceof OptionsScreen
         || mc.screen instanceof PeekScreen
         || mc.screen instanceof ChatScreen
         || mc.screen instanceof InventoryScreen
         || mc.screen instanceof ClickGuiScreen
         || mc.screen instanceof PauseScreen;
   }

   public static boolean isHoldingWeapon(Player player) {
      ItemStack stack = player.getMainHandItem();
      return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.getItem() instanceof MaceItem || stack.getItem() instanceof TridentItem;
   }

   public static boolean isInsideBlock(Player player) {
      return BlockUtil.canCollide(player, player.getBoundingBox());
   }

   public static boolean isInsideBlock() {
      return isInsideBlock(mc.player);
   }

   public static int getDamagePercent(ItemStack stack) {
      return stack.getDamageValue() == stack.getMaxDamage()
         ? 100
         : (int)((stack.getMaxDamage() - stack.getDamageValue()) / Math.max(0.1, (double)stack.getMaxDamage()) * 100.0);
   }

   public static boolean isArmorLow(Player player, int durability) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack piece = player.getItemBySlot(slot);
         if (piece == null || piece.isEmpty()) {
            return true;
         }

         if (getDamagePercent(piece) < durability) {
            return true;
         }
      }

      return false;
   }

   public static void resetLastAttackedTicks() {
      try {
         Field field = LivingEntity.class.getDeclaredField("lastAttackTime");
         field.setAccessible(true);
         field.setInt(mc.player, 0);
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   public static float getHealth(Entity entity) {
      if (entity.showVehicleHealth()) {
         LivingEntity livingBase = (LivingEntity)entity;
         return livingBase.getHealth() + livingBase.getAbsorptionAmount();
      } else {
         return 0.0F;
      }
   }

   public static BlockPos getEntityPos(Entity entity) {
      return new BlockPosX(entity.position());
   }

   public static BlockPos getPlayerPos(boolean fix) {
      return new BlockPosX(mc.player.position(), fix);
   }

   public static BlockPos getEntityPos(Entity entity, boolean fix) {
      return new BlockPosX(entity.position(), fix);
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      Vec3 testVec = pos.getCenter().add(side.getUnitVec3i().getX() * 0.5, side.getUnitVec3i().getY() * 0.5, side.getUnitVec3i().getZ() * 0.5);
      BlockHitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), testVec, Block.COLLIDER, Fluid.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   public static void swingHand(InteractionHand hand, SwingSide side) {
      switch (side) {
         case All:
            mc.player.swing(hand);
            break;
         case Client:
            mc.player.swing(hand, false);
            break;
         case Server:
            mc.getConnection().send(new ServerboundSwingPacket(hand));
      }
   }

   public static void syncInventory() {
      if (AntiCheat.INSTANCE.closeScreen.getValue()) {
         mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      }
   }
}
