package dev.luminous.api.utils.world;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.AutoWeb;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.settings.enums.Placement;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlockUtil implements Wrapper {
   public static final List<BlockPos> placedPos = new ArrayList<>();
   private static final double MIN_EYE_HEIGHT = 0.4;
   private static final double MAX_EYE_HEIGHT = 1.62;
   private static final double MOVEMENT_THRESHOLD = 2.0E-4;

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, 1000.0);
   }

   public static boolean canPlace(BlockPos pos, double getDistance) {
      if (getPlaceSide(pos, getDistance) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, false);
      }
   }

   public static boolean canPlace(BlockPos pos, double getDistance, boolean ignoreCrystal) {
      if (getPlaceSide(pos, getDistance) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
      }
   }

   public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
      return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
   }

   public static List<Entity> getEntities(AABB box) {
      ArrayList<Entity> list = new ArrayList<>();

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (entity != null && (!(entity instanceof ArmorStand) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue()) && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static List<EndCrystal> getEndCrystals(AABB box) {
      ArrayList<EndCrystal> list = new ArrayList<>();

      for (Entity entity : Alien.THREAD.getEntities()) {
         EndCrystal crystal;
         if (entity instanceof EndCrystal && (crystal = (EndCrystal)entity).getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      return hasEntity(new AABB(pos), ignoreCrystal);
   }

   public static boolean hasEntity(AABB box, boolean ignoreCrystal) {
      for (Entity entity : getEntities(box)) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof ThrownExperienceBottle)
            && !(entity instanceof Arrow)
            && (
               !ignoreCrystal
                  || !(entity instanceof EndCrystal)
                  || !(mc.player.getEyePosition().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue())
            )) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for (Entity entity : getEndCrystals(new AABB(pos))) {
         if (entity.isAlive() && entity instanceof EndCrystal) {
            return true;
         }
      }

      return false;
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      return noEntityBlockCrystal(pos, ignoreCrystal, false);
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : getEntities(new AABB(pos))) {
         if (entity.isAlive()
            && (!ignoreItem || !(entity instanceof ItemEntity))
            && (
               !ignoreCrystal
                  || !(entity instanceof EndCrystal)
                  || !(mc.player.getEyePosition().distanceTo(MathUtil.getClosestPoint(entity)) <= AntiCheat.INSTANCE.ieRange.getValue())
            )) {
            return false;
         }
      }

      return true;
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.below();
      BlockPos boost = obsPos.above();
      return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN)
         && getClickSideStrict(obsPos) != null
         && mc.level.isEmptyBlock(boost)
         && noEntityBlockCrystal(boost, false)
         && noEntityBlockCrystal(boost.above(), false)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.level.isEmptyBlock(boost.above()));
   }

   public static void placeCrystal(BlockPos pos, boolean rotate) {
      boolean offhand = mc.player.getOffhandItem().getItem() == Items.END_CRYSTAL;
      BlockPos obsPos = pos.below();
      Direction facing = getClickSide(obsPos);
      Vec3 vec = obsPos.getCenter().add(facing.getUnitVec3i().getX() * 0.5, facing.getUnitVec3i().getY() * 0.5, facing.getUnitVec3i().getZ() * 0.5);
      if (rotate) {
         Alien.ROTATION.lookAt(vec);
      }

      clickBlock(obsPos, facing, false, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
   }

   public static void placeBlock(BlockPos pos, boolean rotate) {
      placeBlock(pos, rotate, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void placeBlock(BlockPos pos, boolean rotate, boolean packet) {
      if (allowAirPlace()) {
         placedPos.add(pos);
         airPlace(pos, rotate, InteractionHand.MAIN_HAND, packet);
      } else {
         Direction side = getPlaceSide(pos);
         if (side != null) {
            placedPos.add(pos);
            clickBlock(pos.relative(side), side.getOpposite(), rotate, InteractionHand.MAIN_HAND, packet);
         }
      }
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
      clickBlock(pos, side, rotate, InteractionHand.MAIN_HAND);
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, InteractionHand hand) {
      clickBlock(pos, side, rotate, hand, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet) {
      clickBlock(pos, side, rotate, InteractionHand.MAIN_HAND, packet);
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, InteractionHand hand, boolean packet) {
      Vec3 directionVec = new Vec3(
         pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
         pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
         pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Module.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, result, id));
      } else {
         mc.gameMode.useItemOn(mc.player, hand, result);
      }

      mc.rightClickDelay = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   public static void clickBlock(BlockPos pos, Direction side, boolean rotate, InteractionHand hand, SwingSide swingSide) {
      Vec3 directionVec = new Vec3(
         pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
         pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
         pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, swingSide);
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      Module.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, result, id));
      mc.rightClickDelay = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   public static void airPlace(BlockPos pos, boolean rotate) {
      airPlace(pos, rotate, InteractionHand.MAIN_HAND, AntiCheat.INSTANCE.packetPlace.getValue());
   }

   public static void airPlace(BlockPos pos, boolean rotate, InteractionHand hand, boolean packet) {
      boolean bypass = hand == InteractionHand.MAIN_HAND && AirPlace.INSTANCE.grimBypass.getValue();
      if (bypass) {
         mc.getConnection().send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
         hand = InteractionHand.OFF_HAND;
      }

      Direction side = getClickSide(pos);
      Vec3 directionVec = new Vec3(
         pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
         pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
         pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
      );
      if (rotate) {
         Alien.ROTATION.lookAt(directionVec);
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         InteractionHand finalHand = hand;
         Module.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(finalHand, result, id));
      } else {
         mc.gameMode.useItemOn(mc.player, hand, result);
      }

      mc.rightClickDelay = 4;
      if (rotate) {
         Alien.ROTATION.snapBack();
      }

      if (bypass) {
         mc.getConnection().send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0), Direction.DOWN));
      }
   }

   public static double distanceToXZ(double x, double z, double x2, double z2) {
      double dx = x2 - x;
      double dz = z2 - z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static double distanceToXZ(double x, double z) {
      return distanceToXZ(x, z, mc.player.getX(), mc.player.getZ());
   }

   public static Direction getPlaceSide(BlockPos pos) {
      if (allowAirPlace()) {
         return getClickSide(pos);
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for (Direction i : Direction.values()) {
            double vecDis;
            if (canClick(pos.relative(i))
               && !canReplace(pos.relative(i))
               && isStrictDirection(pos.relative(i), i.getOpposite())
               && !(
                  (
                        vecDis = mc.player
                           .getEyePosition()
                           .distanceToSqr(pos.getCenter().add(i.getUnitVec3i().getX() * 0.5, i.getUnitVec3i().getY() * 0.5, i.getUnitVec3i().getZ() * 0.5))
                     )
                     > minDistance
               )) {
               side = i;
               minDistance = vecDis;
            }
         }

         return side;
      }
   }

   public static Direction getBestNeighboring(BlockPos pos, Direction facing) {
      Direction bestFacing = null;
      double getDistance = 0.0;

      for (Direction i : Direction.values()) {
         if ((facing == null || !pos.relative(i).equals(pos.relative(facing, -1)))
            && i != Direction.DOWN
            && getPlaceSide(pos) != null
            && (bestFacing == null || mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter()) < getDistance)) {
            bestFacing = i;
            getDistance = mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter());
         }
      }

      return bestFacing;
   }

   public static Direction getPlaceSide(BlockPos pos, double reachDistance) {
      if (allowAirPlace()) {
         Direction i = getClickSide(pos);
         double vecDis = mc.player
            .getEyePosition()
            .distanceToSqr(pos.getCenter().add(i.getUnitVec3i().getX() * 0.5, i.getUnitVec3i().getY() * 0.5, i.getUnitVec3i().getZ() * 0.5));
         return Math.sqrt(vecDis) > reachDistance ? null : Direction.DOWN;
      } else {
         double minDistance = Double.MAX_VALUE;
         Direction side = null;

         for (Direction i : Direction.values()) {
            double vecDis;
            if (canClick(pos.relative(i))
               && !canReplace(pos.relative(i))
               && isStrictDirection(pos.relative(i), i.getOpposite())
               && !(
                  Math.sqrt(
                        vecDis = mc.player
                           .getEyePosition()
                           .distanceToSqr(pos.getCenter().add(i.getUnitVec3i().getX() * 0.5, i.getUnitVec3i().getY() * 0.5, i.getUnitVec3i().getZ() * 0.5))
                     )
                     > reachDistance
               )
               && !(vecDis > minDistance)) {
               side = i;
               minDistance = vecDis;
            }
         }

         return side;
      }
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction side = Direction.UP;
      double minDistance = Double.MAX_VALUE;

      for (Direction i : Direction.values()) {
         double disSq;
         if (isStrictDirection(pos, i) && !((disSq = mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > minDistance)) {
            side = i;
            minDistance = disSq;
         }
      }

      return side;
   }

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction side = null;
      double minDistance = Double.MAX_VALUE;

      for (Direction i : Direction.values()) {
         double disSq;
         if (isStrictDirection(pos, i) && !((disSq = mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > minDistance)) {
            side = i;
            minDistance = disSq;
         }
      }

      return side;
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side, double reachDistance) {
      double vecDis = mc.player
         .getEyePosition()
         .distanceToSqr(pos.getCenter().add(side.getUnitVec3i().getX() * 0.5, side.getUnitVec3i().getY() * 0.5, side.getUnitVec3i().getZ() * 0.5));
      return Math.sqrt(vecDis) > reachDistance ? false : isStrictDirection(pos, side);
   }

   public static boolean isStrictDirection(BlockPos pos, Direction side) {
      switch ((Placement)AntiCheat.INSTANCE.placement.getValue()) {
         case Vanilla:
            return true;
         case Legit:
            return EntityUtil.canSee(pos, side);
         case Grim:
            return grimStrictDirectionCheck(pos, side, mc.level, mc.player);
         case NCP:
            if (mc.level.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(mc.level, pos.relative(side))) {
               return false;
            }

            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 blockCenter = pos.getCenter();
            ArrayList<Direction> validAxis = new ArrayList<>();
            validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false));
            validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
            validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false));
            return validAxis.contains(side);
         default:
            return true;
      }
   }

   public static boolean grimStrictDirectionCheck(BlockPos pos, Direction direction, ClientLevel level, LocalPlayer player) {
      AABB combined = getCombinedBox(pos, level);
      AABB eyePositions = new AABB(player.getX(), player.getY() + 0.4, player.getZ(), player.getX(), player.getY() + 1.62, player.getZ()).inflate(2.0E-4);
      if (isIntersected(eyePositions, combined)) {
         return true;
      } else {
         switch (direction) {
            case NORTH:
               if (!(eyePositions.minZ > combined.minZ)) {
                  return true;
               }
               break;
            case SOUTH:
               if (!(eyePositions.maxZ < combined.maxZ)) {
                  return true;
               }
               break;
            case EAST:
               if (!(eyePositions.maxX < combined.maxX)) {
                  return true;
               }
               break;
            case WEST:
               if (!(eyePositions.minX > combined.minX)) {
                  return true;
               }
               break;
            case UP:
               if (!(eyePositions.maxY < combined.maxY)) {
                  return true;
               }
               break;
            case DOWN:
               if (!(eyePositions.minY > combined.minY)) {
                  return true;
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return false;
      }
   }

   private static AABB getCombinedBox(BlockPos pos, Level level) {
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
      AABB combined = new AABB(pos);

      for (AABB box : shape.toAabbs()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   private static boolean isIntersected(AABB bb, AABB other) {
      return other.maxX - 1.0E-7 > bb.minX
         && other.minX + 1.0E-7 < bb.maxX
         && other.maxY - 1.0E-7 > bb.minY
         && other.minY + 1.0E-7 < bb.maxY
         && other.maxZ - 1.0E-7 > bb.minZ
         && other.minZ + 1.0E-7 < bb.maxZ;
   }

   public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean vertical) {
      ArrayList<Direction> valid = new ArrayList<>();
      if (vertical) {
         if (diff < -0.5) {
            valid.add(negativeSide);
         }

         if (AntiCheat.INSTANCE.upDirectionLimit.getValue()) {
            if (diff > 0.5) {
               valid.add(positiveSide);
            }
         } else if (diff > -0.5) {
            valid.add(positiveSide);
         }
      } else {
         if (diff < -0.5) {
            valid.add(negativeSide);
         }

         if (diff > 0.5) {
            valid.add(positiveSide);
         }
      }

      return valid;
   }

   public static ArrayList<BlockEntity> getTileEntities() {
      return getLoadedChunks().flatMap(chunk -> chunk.getBlockEntities().values().stream()).collect(Collectors.toCollection(ArrayList::new));
   }

   public static Stream<LevelChunk> getLoadedChunks() {
      int radius = Math.max(2, mc.options.getEffectiveRenderDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = mc.player.chunkPosition();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.<ChunkPos>iterate(min, pos -> {
         int x = pos.x;
         int z = pos.z;
         if (++x > max.x) {
            x = min.x;
            z++;
         }

         return new ChunkPos(x, z);
      }).limit((long)diameter * diameter).filter(c -> mc.level.hasChunk(c.x, c.z)).map(c -> mc.level.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }

   public static ArrayList<BlockPos> getSphere(float range) {
      return getSphere(range, mc.player.getEyePosition());
   }

   public static BlockPos getBlock(Block block, float range) {
      for (BlockPos pos : getSphere(range)) {
         if (mc.level.getBlockState(pos).getBlock() == block) {
            return pos;
         }
      }

      return null;
   }

   public static BlockPos getBlock(Class<?> block, float range) {
      for (BlockPos pos : getSphere(range)) {
         if (block.isInstance(mc.level.getBlockState(pos).getBlock())) {
            return pos;
         }
      }

      return null;
   }

   public static ArrayList<BlockPos> getSphere(float range, Vec3 pos) {
      ArrayList<BlockPos> list = new ArrayList<>();

      for (double y = pos.y() + range; y > pos.y() - range; y--) {
         if (!(y < -64.0)) {
            for (double x = pos.x() - range; x < pos.x() + range; x++) {
               for (double z = pos.z() - range; z < pos.z() + range; z++) {
                  BlockPosX curPos = new BlockPosX(x, y, z);
                  if (!(curPos.getCenter().distanceTo(pos) > range)) {
                     list.add(curPos);
                  }
               }
            }
         }
      }

      return list;
   }

   public static Block getBlock(BlockPos pos) {
      return mc.level.getBlockState(pos).getBlock();
   }

   public static boolean canReplace(BlockPos pos) {
      if (pos.getY() >= 320) {
         return false;
      } else if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return false;
      } else {
         BlockState state = mc.level.getBlockState(pos);
         return state.getBlock() == Blocks.COBWEB && AutoWeb.ignore && AutoCrystal.INSTANCE.replace.getValue() ? true : state.canBeReplaced();
      }
   }

   public static boolean canClick(BlockPos pos) {
      if (AntiCheat.INSTANCE.multiPlace.getValue() && placedPos.contains(pos)) {
         return true;
      } else {
         BlockState state = mc.level.getBlockState(pos);
         Block block = state.getBlock();
         return block == Blocks.COBWEB && AutoWeb.ignore ? AutoCrystal.INSTANCE.airPlace.getValue() : mc.player.isShiftKeyDown() || !isClickable(block);
      }
   }

   public static boolean isClickable(Block block) {
      return block instanceof CraftingTableBlock
         || block instanceof AnvilBlock
         || block instanceof LoomBlock
         || block instanceof CartographyTableBlock
         || block instanceof GrindstoneBlock
         || block instanceof StonecutterBlock
         || block instanceof ButtonBlock
         || block instanceof BasePressurePlateBlock
         || block instanceof BaseEntityBlock
         || block instanceof BedBlock
         || block instanceof FenceGateBlock
         || block instanceof DoorBlock
         || block instanceof NoteBlock
         || block instanceof TrapDoorBlock;
   }

   public static boolean canCollide(AABB box) {
      return canCollide(mc.player, box);
   }

   public static boolean canCollide(@Nullable Entity entity, AABB box) {
      BlockCollisions blockCollisionSpliterator = new BlockCollisions(mc.level, entity, box, false, (pos, voxelShape) -> voxelShape);

      while (blockCollisionSpliterator.hasNext()) {
         if (!((VoxelShape)blockCollisionSpliterator.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public static boolean allowAirPlace() {
      return AirPlace.INSTANCE.isOn() && AirPlace.INSTANCE.module.getValue();
   }
}
