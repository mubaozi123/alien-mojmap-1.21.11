package dev.luminous.api.utils.math;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.Criticals;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.commons.lang3.mutable.MutableInt;

public class DamageUtils {
   public static final DamageUtils.RaycastFactory HIT_FACTORY = (context, blockPos) -> {
      BlockState blockState = Wrapper.mc.level.getBlockState(blockPos);
      return blockState.getBlock().getExplosionResistance() < 600.0F
         ? null
         : blockState.getCollisionShape(Wrapper.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
   };

   public static float calculateDamage(BlockPos pos, LivingEntity entity) {
      return explosionDamage(entity, null, new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), 12.0F);
   }

   public static float calculateDamage(Vec3 pos, LivingEntity entity) {
      return explosionDamage(entity, null, pos, 12.0F);
   }

   public static float explosionDamage(
      LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float power, DamageUtils.RaycastFactory raycastFactory
   ) {
      double modDistance = distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
      if (modDistance > power) {
         return 0.0F;
      } else {
         double exposure = getExposure(explosionPos, targetBox, raycastFactory);
         double impact = (1.0 - modDistance / power) * exposure;
         float damage = (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
         return calculateReductionsExplosion(damage, target, Wrapper.mc.level.damageSources().explosion(null));
      }
   }

   public static float anchorDamage(LivingEntity target, LivingEntity predict, Vec3 anchor) {
      return overridingExplosionDamage(target, predict, anchor, 10.0F, BlockPos.containing(anchor), Blocks.AIR.defaultBlockState());
   }

   public static float overridingExplosionDamage(
      LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power, BlockPos overridePos, BlockState overrideState
   ) {
      return explosionDamage(target, predict, explosionPos, power, getOverridingHitFactory(overridePos, overrideState));
   }

   private static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power, DamageUtils.RaycastFactory raycastFactory) {
      if (target == null) {
         return 0.0F;
      } else {
         return target instanceof Player player && getGameMode(player) == GameType.CREATIVE
            ? 0.0F
            : explosionDamage(
               target,
               predict != null ? predict.position() : target.position(),
               predict != null ? predict.getBoundingBox() : target.getBoundingBox(),
               explosionPos,
               power,
               raycastFactory
            );
      }
   }

   public static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power) {
      if (target == null) {
         return 0.0F;
      } else {
         return target instanceof Player player && getGameMode(player) == GameType.CREATIVE
            ? 0.0F
            : explosionDamage(
               target,
               predict != null ? predict.position() : target.position(),
               predict != null ? predict.getBoundingBox() : target.getBoundingBox(),
               explosionPos,
               power,
               HIT_FACTORY
            );
      }
   }

   public static DamageUtils.RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
      return (context, blockPos) -> {
         BlockState blockState;
         if (blockPos.equals(overridePos)) {
            blockState = overrideState;
         } else {
            blockState = Wrapper.mc.level.getBlockState(blockPos);
            if (blockState.getBlock().getExplosionResistance() < 600.0F) {
               return null;
            }
         }

         return blockState.getCollisionShape(Wrapper.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
      };
   }

   public static float getAttackDamage(LivingEntity attacker, LivingEntity target) {
      float itemDamage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
      DamageSource damageSource = attacker instanceof Player player
         ? Wrapper.mc.level.damageSources().playerAttack(player)
         : Wrapper.mc.level.damageSources().mobAttack(attacker);
      MobEffectInstance effect = attacker.getEffect(MobEffects.STRENGTH);
      if (effect != null) {
         itemDamage += 3.0F * (effect.getAmplifier() + 1);
      }

      float damage = modifyAttackDamage(attacker, target, attacker.getWeaponItem(), damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   public static float getAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
      AttributeInstance original = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
      AttributeInstance copy = new AttributeInstance(Attributes.ATTACK_DAMAGE, o -> {});
      copy.setBaseValue(original.getBaseValue());

      for (AttributeModifier modifier : original.getModifiers()) {
         copy.addTransientModifier(modifier);
      }

      copy.removeModifier(Item.BASE_ATTACK_DAMAGE_ID);
      ItemAttributeModifiers attributeModifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (attributeModifiers != null) {
         attributeModifiers.forEach(EquipmentSlot.MAINHAND, (entry, modifier) -> {
            if (entry == Attributes.ATTACK_DAMAGE) {
               copy.addOrUpdateTransientModifier(modifier);
            }
         });
      }

      float itemDamage = (float)copy.getValue();
      DamageSource damageSource = attacker instanceof Player player
         ? Wrapper.mc.level.damageSources().playerAttack(player)
         : Wrapper.mc.level.damageSources().mobAttack(attacker);
      float damage = modifyAttackDamage(attacker, target, weapon, damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   private static float modifyAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, DamageSource damageSource, float damage) {
      Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();
      getEnchantments(weapon, enchantments);
      float enchantDamage = 0.0F;
      int sharpness = getEnchantmentLevel(enchantments, Enchantments.SHARPNESS);
      if (sharpness > 0) {
         enchantDamage += 1.0F + 0.5F * (sharpness - 1);
      }

      int baneOfArthropods = getEnchantmentLevel(enchantments, Enchantments.BANE_OF_ARTHROPODS);
      if (baneOfArthropods > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
         enchantDamage += 2.5F * baneOfArthropods;
      }

      int impaling = getEnchantmentLevel(enchantments, Enchantments.IMPALING);
      if (impaling > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
         enchantDamage += 2.5F * impaling;
      }

      int smite = getEnchantmentLevel(enchantments, Enchantments.SMITE);
      if (smite > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
         enchantDamage += 2.5F * smite;
      }

      if (attacker instanceof Player playerEntity) {
         float charge = playerEntity.getAttackStrengthScale(0.5F);
         damage *= 0.2F + charge * charge * 0.8F;
         enchantDamage *= charge;
         if (weapon.getItem() instanceof MaceItem item) {
            float bonusDamage = item.getAttackDamageBonus(target, damage, damageSource);
            if (bonusDamage > 0.0F) {
               int density = getEnchantmentLevel(weapon, Enchantments.DENSITY);
               if (density > 0) {
                  bonusDamage = (float)(bonusDamage + 0.5 * attacker.fallDistance);
               }

               damage += bonusDamage;
            }
         }

         if (charge > 0.9F
            && (
               attacker.fallDistance > 0.0
                  || attacker == Wrapper.mc.player
                     && Criticals.INSTANCE.isOn()
                     && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground)
                     && (Wrapper.mc.player.onGround() || !Criticals.INSTANCE.onlyGround.getValue())
            )
            && (!attacker.onGround() || attacker == Wrapper.mc.player && Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground))
            && !attacker.onClimbable()
            && !attacker.isInWater()
            && !attacker.hasEffect(MobEffects.BLINDNESS)
            && !attacker.isPassenger()) {
            damage *= 1.5F;
         }
      }

      return damage + enchantDamage;
   }

   public static float fallDamage(LivingEntity entity) {
      if (entity instanceof Player player && player.getAbilities().flying) {
         return 0.0F;
      } else if (!entity.hasEffect(MobEffects.SLOW_FALLING) && !entity.hasEffect(MobEffects.LEVITATION)) {
         int surface = Wrapper.mc
            .level
            .getChunkAt(entity.blockPosition())
            .getOrCreateHeightmapUnprimed(Types.MOTION_BLOCKING)
            .getFirstAvailable(entity.getBlockX() & 15, entity.getBlockZ() & 15);
         if (entity.getBlockY() >= surface) {
            return fallDamageReductions(entity, surface);
         } else {
            BlockHitResult raycastResult = Wrapper.mc
               .level
               .clip(
                  new ClipContext(entity.position(), new Vec3(entity.getX(), Wrapper.mc.level.getMinY(), entity.getZ()), Block.COLLIDER, Fluid.WATER, entity)
               );
            return raycastResult.getType() == Type.MISS ? 0.0F : fallDamageReductions(entity, raycastResult.getBlockPos().getY());
         }
      } else {
         return 0.0F;
      }
   }

   private static float fallDamageReductions(LivingEntity entity, int surface) {
      int fallHeight = (int)(entity.getY() - surface + entity.fallDistance - 3.0);
      MobEffectInstance jumpBoostInstance = entity.getEffect(MobEffects.JUMP_BOOST);
      if (jumpBoostInstance != null) {
         fallHeight -= jumpBoostInstance.getAmplifier() + 1;
      }

      return calculateReductions(fallHeight, entity, Wrapper.mc.level.damageSources().fall());
   }

   public static float calculateReductionsExplosion(float damage, LivingEntity entity, DamageSource damageSource) {
      if (damageSource.scalesWithDifficulty()) {
         switch (Wrapper.mc.level.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      damage = CombatRules.getDamageAfterAbsorb(entity, damage, damageSource, getArmor(entity), (float)getARMOR_TOUGHNESS(entity));
      damage = resistanceReduction(entity, damage);
      damage = CombatRules.getDamageAfterMagicAbsorb(
         damage,
         getProtectionAmount(
            List.of(
               entity.getItemBySlot(EquipmentSlot.HEAD),
               entity.getItemBySlot(EquipmentSlot.CHEST),
               entity.getItemBySlot(EquipmentSlot.LEGS),
               entity.getItemBySlot(EquipmentSlot.FEET)
            )
         )
      );
      return Math.max(damage, 0.0F);
   }

   public static float calculateReductions(float damage, LivingEntity entity, DamageSource damageSource) {
      if (damageSource.scalesWithDifficulty()) {
         switch (Wrapper.mc.level.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      damage = CombatRules.getDamageAfterAbsorb(entity, damage, damageSource, getArmor(entity), (float)getARMOR_TOUGHNESS(entity));
      damage = resistanceReduction(entity, damage);
      damage = protectionReduction(entity, damage, damageSource);
      return Math.max(damage, 0.0F);
   }

   public static double getARMOR_TOUGHNESS(LivingEntity entity) {
      if (entity instanceof Player player) {
         PlayerManager.EntityAttribute entityAttribute = Alien.PLAYER.map.get(player);
         if (entityAttribute != null) {
            return entityAttribute.toughness();
         }
      }

      return entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
   }

   private static float getArmor(LivingEntity entity) {
      if (entity instanceof Player player) {
         PlayerManager.EntityAttribute entityAttribute = Alien.PLAYER.map.get(player);
         if (entityAttribute != null) {
            return entityAttribute.armor();
         }
      }

      return (float)Math.floor(entity.getAttributeValue(Attributes.ARMOR));
   }

   private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return damage;
      } else {
         int damageProtection = 0;

         for (ItemStack stack : List.of(
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
         )) {
            Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();
            getEnchantments(stack, enchantments);
            int protection = getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            if (protection > 0) {
               damageProtection += protection;
            }

            int fireProtection = getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0 && source.is(DamageTypeTags.IS_FIRE)) {
               damageProtection += 2 * fireProtection;
            }

            int blastProtection = getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
            if (blastProtection > 0 && source.is(DamageTypeTags.IS_EXPLOSION)) {
               damageProtection += 2 * blastProtection;
            }

            int projectileProtection = getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
            if (projectileProtection > 0 && source.is(DamageTypeTags.IS_PROJECTILE)) {
               damageProtection += 2 * projectileProtection;
            }

            int featherFalling = getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING);
            if (featherFalling > 0 && source.is(DamageTypeTags.IS_FALL)) {
               damageProtection += 3 * featherFalling;
            }
         }

         return CombatRules.getDamageAfterMagicAbsorb(damage, damageProtection);
      }
   }

   public static int getProtectionAmount(Iterable<ItemStack> equipment) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
      return mutableInt.intValue();
   }

   public static int getProtectionAmount(ItemStack stack) {
      int modifierBlast = EnchantmentHelper.getItemEnchantmentLevel(
         Wrapper.mc.level.registryAccess().lookupOrThrow(Enchantments.BLAST_PROTECTION.registryKey()).getOrThrow(Enchantments.BLAST_PROTECTION), stack
      );
      int modifier = EnchantmentHelper.getItemEnchantmentLevel(
         Wrapper.mc.level.registryAccess().lookupOrThrow(Enchantments.PROTECTION.registryKey()).getOrThrow(Enchantments.PROTECTION), stack
      );
      return modifierBlast * 2 + modifier;
   }

   private static float resistanceReduction(LivingEntity player, float damage) {
      MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
      if (resistance != null) {
         int lvl = resistance.getAmplifier() + 1;
         damage *= 1.0F - lvl * 0.2F;
      }

      return Math.max(damage, 0.0F);
   }

   private static float getExposure(Vec3 source, AABB box, DamageUtils.RaycastFactory raycastFactory) {
      if (ClientSetting.INSTANCE.optimizedCalc.getValue()) {
         int miss = 0;
         int hit = 0;

         for (int k = 0; k <= 1; k++) {
            for (int l = 0; l <= 1; l++) {
               for (int m = 0; m <= 1; m++) {
                  double n = Mth.lerp((double)k, box.minX, box.maxX);
                  double o = Mth.lerp((double)l, box.minY, box.maxY);
                  double p = Mth.lerp((double)m, box.minZ, box.maxZ);
                  Vec3 vec3d = new Vec3(n, o, p);
                  if (raycast(vec3d, source, CombatUtil.terrainIgnore) == Type.MISS) {
                     miss++;
                  }

                  hit++;
               }
            }
         }

         return (float)miss / hit;
      } else {
         double xDiff = box.maxX - box.minX;
         double yDiff = box.maxY - box.minY;
         double zDiff = box.maxZ - box.minZ;
         double xStep = 1.0 / (xDiff * 2.0 + 1.0);
         double yStep = 1.0 / (yDiff * 2.0 + 1.0);
         double zStep = 1.0 / (zDiff * 2.0 + 1.0);
         if (xStep > 0.0 && yStep > 0.0 && zStep > 0.0) {
            int misses = 0;
            int hits = 0;
            double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
            double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
            xStep *= xDiff;
            yStep *= yDiff;
            zStep *= zDiff;
            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
               for (double y = startY; y <= endY; y += yStep) {
                  for (double z = startZ; z <= endZ; z += zStep) {
                     Vec3 position = new Vec3(x, y, z);
                     if (raycast(new DamageUtils.ExposureRaycastContext(position, source), raycastFactory) == null) {
                        misses++;
                     }

                     hits++;
                  }
               }
            }

            return (float)misses / hits;
         } else {
            return 0.0F;
         }
      }
   }

   public static Type raycast(Vec3 start, Vec3 end, boolean ignoreTerrain) {
      return BlockGetter.traverseBlocks(start, end, null, (innerContext, blockPos) -> {
         BlockState blockState = Wrapper.mc.level.getBlockState(blockPos);
         if (blockState.getBlock().getExplosionResistance() < 600.0F && ignoreTerrain) {
            return null;
         } else {
            BlockHitResult hitResult = blockState.getCollisionShape(Wrapper.mc.level, blockPos).clip(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
         }
      }, innerContext -> Type.MISS);
   }

   public static BlockHitResult raycast(DamageUtils.ExposureRaycastContext context, DamageUtils.RaycastFactory raycastFactory) {
      return BlockGetter.traverseBlocks(context.start(), context.end(), context, raycastFactory, ctx -> null);
   }

   public static int getEnchantmentLevel(ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
      if (itemStack.isEmpty()) {
         return 0;
      } else {
         Object2IntMap<Holder<Enchantment>> itemEnchantments = new Object2IntArrayMap();
         getEnchantments(itemStack, itemEnchantments);
         return getEnchantmentLevel(itemEnchantments, enchantment);
      }
   }

   public static int getEnchantmentLevel(Object2IntMap<Holder<Enchantment>> itemEnchantments, ResourceKey<Enchantment> enchantment) {
      ObjectIterator var2 = Object2IntMaps.fastIterable(itemEnchantments).iterator();

      while (var2.hasNext()) {
         Entry<Holder<Enchantment>> entry = (Entry<Holder<Enchantment>>)var2.next();
         if (((Holder)entry.getKey()).is(enchantment)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
   }

   public static GameType getGameMode(Player player) {
      if (player == null) {
         return null;
      } else {
         PlayerInfo playerListEntry = Wrapper.mc.getConnection().getPlayerInfo(player.getUUID());
         return playerListEntry == null ? null : playerListEntry.getGameMode();
      }
   }

   public static double squaredDistanceTo(Entity entity) {
      return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
   }

   public static double squaredDistanceTo(BlockPos blockPos) {
      return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   public static double squaredDistanceTo(double x, double y, double z) {
      return squaredDistance(Wrapper.mc.player.getX(), Wrapper.mc.player.getY(), Wrapper.mc.player.getZ(), x, y, z);
   }

   public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double f = x1 - x2;
      double g = y1 - y2;
      double h = z1 - z2;
      return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
   }

   public static void getEnchantments(ItemStack itemStack, Object2IntMap<Holder<Enchantment>> enchantments) {
      enchantments.clear();
      if (!itemStack.isEmpty()) {
         for (Entry<Holder<Enchantment>> entry : itemStack.getItem() == Items.ENCHANTED_BOOK
            ? itemStack.get(DataComponents.STORED_ENCHANTMENTS).entrySet()
            : itemStack.getEnchantments().entrySet()) {
            enchantments.put((Holder)entry.getKey(), entry.getIntValue());
         }
      }
   }

   public record ExposureRaycastContext(Vec3 start, Vec3 end) {
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<DamageUtils.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
