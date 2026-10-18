package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.AABB;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   private final BooleanSetting pistonPacket = this.add(new BooleanSetting("PistonPacket", false));
   private final BooleanSetting noEating = this.add(new BooleanSetting("NoEating", true));
   private final BooleanSetting eatingBreak = this.add(new BooleanSetting("EatingBreak", false));
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 1.0, 8.0));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 1.0, 8.0));
   private final BooleanSetting fire = this.add(new BooleanSetting("Fire", true));
   private final BooleanSetting switchPos = this.add(new BooleanSetting("Switch", false));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("SelfGround", true));
   private final BooleanSetting onlyStatic = this.add(new BooleanSetting("MovingPause", true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 500));
   private final SliderSetting posUpdateDelay = this.add(new SliderSetting("PosUpdateDelay", 500, 0, 1000));
   private final SliderSetting stageSetting = this.add(new SliderSetting("Stage", 4, 1, 10));
   private final SliderSetting pistonStage = this.add(new SliderSetting("PistonStage", 1, 1, 10));
   private final SliderSetting pistonMaxStage = this.add(new SliderSetting("PistonMaxStage", 1, 1, 10));
   private final SliderSetting powerStage = this.add(new SliderSetting("PowerStage", 3, 1, 10));
   private final SliderSetting powerMaxStage = this.add(new SliderSetting("PowerMaxStage", 3, 1, 10));
   private final SliderSetting crystalStage = this.add(new SliderSetting("CrystalStage", 4, 1, 10));
   private final SliderSetting crystalMaxStage = this.add(new SliderSetting("CrystalMaxStage", 4, 1, 10));
   private final SliderSetting fireStage = this.add(new SliderSetting("FireStage", 2, 1, 10));
   private final SliderSetting fireMaxStage = this.add(new SliderSetting("FireMaxStage", 2, 1, 10));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting debug = this.add(new BooleanSetting("Debug", false));
   private final Timer timer = new Timer();
   private final Timer crystalTimer = new Timer();
   public BlockPos bestPos = null;
   public BlockPos bestOPos = null;
   public Direction bestFacing = null;
   public double distance = 100.0;
   public boolean getPos = false;
   public int stage = 1;
   private Player target = null;
   private boolean isPiston = false;

   public PistonCrystal() {
      super("PistonCrystal", Module.Category.Combat);
      this.setChinese("活塞水晶");
      INSTANCE = this;
   }

   private static boolean canFire(BlockPos pos) {
      if (BlockUtil.canReplace(pos.below())) {
         return false;
      } else if (!mc.level.isEmptyBlock(pos)) {
         return false;
      } else {
         return !BlockUtil.canClick(pos.relative(Direction.DOWN)) ? false : BlockUtil.isStrictDirection(pos.below(), Direction.UP);
      }
   }

   public void onTick() {
      if (this.pistonStage.getValue() > this.stageSetting.getValue()) {
         this.pistonStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireStage.getValue() > this.stageSetting.getValue()) {
         this.fireStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerStage.getValue() > this.stageSetting.getValue()) {
         this.powerStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalStage.getValue() > this.stageSetting.getValue()) {
         this.crystalStage.setValue(this.stageSetting.getValue());
      }

      if (this.pistonMaxStage.getValue() > this.stageSetting.getValue()) {
         this.pistonMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.fireMaxStage.getValue() > this.stageSetting.getValue()) {
         this.fireMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.powerMaxStage.getValue() > this.stageSetting.getValue()) {
         this.powerMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() > this.stageSetting.getValue()) {
         this.crystalMaxStage.setValue(this.stageSetting.getValue());
      }

      if (this.crystalMaxStage.getValue() < this.crystalStage.getValue()) {
         this.crystalStage.setValue(this.crystalMaxStage.getValue());
      }

      if (this.powerMaxStage.getValue() < this.powerStage.getValue()) {
         this.powerStage.setValue(this.powerMaxStage.getValue());
      }

      if (this.pistonMaxStage.getValue() < this.pistonStage.getValue()) {
         this.pistonStage.setValue(this.pistonMaxStage.getValue());
      }

      if (this.fireMaxStage.getValue() < this.fireStage.getValue()) {
         this.fireStage.setValue(this.fireMaxStage.getValue());
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.onTick();
      this.target = CombatUtil.getClosestEnemy(this.range.getValue());
      if (this.target != null) {
         if (!this.noEating.getValue() || !mc.player.isUsingItem()) {
            if (!this.check(this.onlyStatic.getValue(), !mc.player.onGround(), this.onlyGround.getValue())) {
               BlockPos pos = EntityUtil.getEntityPos(this.target, true);
               if (!mc.player.isUsingItem() || this.eatingBreak.getValue()) {
                  if (this.checkCrystal(pos.above(0))) {
                     CombatUtil.attackCrystal(pos.above(0), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.above(1))) {
                     CombatUtil.attackCrystal(pos.above(1), this.rotate.getValue(), true);
                  }

                  if (this.checkCrystal(pos.above(2))) {
                     CombatUtil.attackCrystal(pos.above(2), this.rotate.getValue(), true);
                  }
               }

               if (this.bestPos != null && mc.level.getBlockState(this.bestPos).getBlock() instanceof PistonBaseBlock) {
                  this.isPiston = true;
               } else if (this.isPiston) {
                  this.isPiston = false;
                  this.crystalTimer.reset();
                  this.bestPos = null;
               }

               if (this.crystalTimer.passedMs(this.posUpdateDelay.getValueInt())) {
                  this.stage = 0;
                  this.distance = 100.0;
                  this.getPos = false;
                  this.getBestPos(pos.above(2));
                  this.getBestPos(pos.above());
               }

               if (this.timer.passedMs(this.updateDelay.getValueInt())) {
                  if (this.getPos && this.bestPos != null) {
                     this.timer.reset();
                     if (this.debug.getValue()) {
                        this.sendMessage(
                           "[Debug] PistonPos:" + this.bestPos + " Facing:" + this.bestFacing + " CrystalPos:" + this.bestOPos.relative(this.bestFacing)
                        );
                     }

                     this.doPistonAura(this.bestPos, this.bestFacing, this.bestOPos);
                  }
               }
            }
         }
      }
   }

   public boolean check(boolean onlyStatic, boolean onGround, boolean onlyGround) {
      if (MovementUtil.isMoving() && onlyStatic) {
         return true;
      } else if (onGround && onlyGround) {
         return true;
      } else if (this.findBlock(Blocks.REDSTONE_BLOCK) == -1) {
         return true;
      } else {
         return this.findClass(PistonBaseBlock.class) == -1 ? true : this.findItem(Items.END_CRYSTAL) == -1;
      }
   }

   private boolean checkCrystal(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new AABB(pos))) {
         if (entity instanceof EndCrystal) {
            float damage = AutoCrystal.INSTANCE.calculateDamage(entity.position(), this.target, this.target);
            if (damage > 7.0F) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean checkCrystal2(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new AABB(pos))) {
         if (entity instanceof EndCrystal && EntityUtil.getEntityPos(entity).equals(pos)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private void getBestPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.DOWN && i != Direction.UP) {
            this.getPos(pos, i);
         }
      }
   }

   private void getPos(BlockPos pos, Direction i) {
      if (BlockUtil.canPlaceCrystal(pos.relative(i)) || this.checkCrystal2(pos.relative(i))) {
         this.getPos(pos.relative(i, 3), i, pos);
         this.getPos(pos.relative(i, 3).above(), i, pos);
         int offsetX = pos.relative(i).getX() - pos.getX();
         int offsetZ = pos.relative(i).getZ() - pos.getZ();
         this.getPos(pos.relative(i, 3).offset(offsetZ, 0, offsetX), i, pos);
         this.getPos(pos.relative(i, 3).offset(-offsetZ, 0, -offsetX), i, pos);
         this.getPos(pos.relative(i, 3).offset(offsetZ, 1, offsetX), i, pos);
         this.getPos(pos.relative(i, 3).offset(-offsetZ, 1, -offsetX), i, pos);
         this.getPos(pos.relative(i, 2), i, pos);
         this.getPos(pos.relative(i, 2).above(), i, pos);
         this.getPos(pos.relative(i, 2).offset(offsetZ, 0, offsetX), i, pos);
         this.getPos(pos.relative(i, 2).offset(-offsetZ, 0, -offsetX), i, pos);
         this.getPos(pos.relative(i, 2).offset(offsetZ, 1, offsetX), i, pos);
         this.getPos(pos.relative(i, 2).offset(-offsetZ, 1, -offsetX), i, pos);
      }
   }

   private void getPos(BlockPos pos, Direction facing, BlockPos oPos) {
      if (!this.switchPos.getValue() || this.bestPos == null || !this.bestPos.equals(pos) || !mc.level.isEmptyBlock(this.bestPos)) {
         if (BlockUtil.canPlace(pos, this.placeRange.getValue()) || this.getBlock(pos) instanceof PistonBaseBlock) {
            if (this.findClass(PistonBaseBlock.class) != -1) {
               if (!ClientSetting.INSTANCE.lowVersion.getValue()
                  || this.getBlock(pos) instanceof PistonBaseBlock
                  || !(mc.player.getY() - pos.getY() <= -2.0) && !(mc.player.getY() - pos.getY() >= 3.0)
                  || !(BlockUtil.distanceToXZ(pos.getX() + 0.5, pos.getZ() + 0.5) < 2.6)) {
                  if (mc.level.isEmptyBlock(pos.relative(facing, -1))
                     && mc.level.getBlockState(pos.relative(facing, -1)).getBlock() != Blocks.FIRE
                     && (this.getBlock(pos.relative(facing.getOpposite())) != Blocks.MOVING_PISTON || this.checkCrystal2(pos.relative(facing.getOpposite())))) {
                     if (BlockUtil.canPlace(pos, this.placeRange.getValue()) || this.isPiston(pos, facing)) {
                        if (Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter())) < this.distance || this.bestPos == null) {
                           this.bestPos = pos;
                           this.bestOPos = oPos;
                           this.bestFacing = facing;
                           this.distance = Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter()));
                           this.getPos = true;
                           this.crystalTimer.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void doPistonAura(BlockPos pos, Direction facing, BlockPos oPos) {
      if (this.stage >= this.stageSetting.getValue()) {
         this.stage = 0;
      }

      this.stage++;
      if (mc.level.isEmptyBlock(pos)) {
         if (!BlockUtil.canPlace(pos)) {
            return;
         }

         if (this.stage >= this.pistonStage.getValue() && this.stage <= this.pistonMaxStage.getValue()) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            int old = mc.player.getInventory().selected;
            AutoPush.pistonFacing(facing);
            int piston = this.findClass(PistonBaseBlock.class);
            this.doSwap(piston);
            BlockUtil.placeBlock(pos, false, this.pistonPacket.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(piston);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }

            BlockPos neighbour = pos.relative(side);
            Direction opposite = side.getOpposite();
            if (this.rotate.getValue()) {
               Alien.ROTATION.lookAt(neighbour, opposite);
            }
         }
      }

      if (this.stage >= this.powerStage.getValue() && this.stage <= this.powerMaxStage.getValue()) {
         this.doRedStone(pos, facing, oPos.relative(facing));
      }

      if (this.stage >= this.crystalStage.getValue() && this.stage <= this.crystalMaxStage.getValue()) {
         this.placeCrystal(oPos, facing);
      }

      if (this.stage >= this.fireStage.getValue() && this.stage <= this.fireMaxStage.getValue()) {
         this.doFire(oPos, facing);
      }
   }

   private void placeCrystal(BlockPos pos, Direction facing) {
      if (BlockUtil.canPlaceCrystal(pos.relative(facing))) {
         int crystal = this.findItem(Items.END_CRYSTAL);
         if (crystal != -1) {
            int old = mc.player.getInventory().selected;
            this.doSwap(crystal);
            BlockUtil.placeCrystal(pos.relative(facing), true);
            if (this.inventory.getValue()) {
               this.doSwap(crystal);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(old);
            }
         }
      }
   }

   private boolean isPiston(BlockPos pos, Direction facing) {
      if (!(mc.level.getBlockState(pos).getBlock() instanceof PistonBaseBlock)) {
         return false;
      } else {
         return mc.level.getBlockState(pos).getValue(DirectionalBlock.FACING).getOpposite() != facing
            ? false
            : mc.level.isEmptyBlock(pos.relative(facing, -1))
               || this.getBlock(pos.relative(facing, -1)) == Blocks.FIRE
               || this.getBlock(pos.relative(facing.getOpposite())) == Blocks.MOVING_PISTON;
      }
   }

   private void doFire(BlockPos pos, Direction facing) {
      if (this.fire.getValue()) {
         int fire = this.findItem(Items.FLINT_AND_STEEL);
         if (fire != -1) {
            int old = mc.player.getInventory().selected;
            int[] xOffset = new int[]{0, facing.getStepZ(), -facing.getStepZ()};
            int[] yOffset = new int[]{0, 1};
            int[] zOffset = new int[]{0, facing.getStepX(), -facing.getStepX()};

            for (int x : xOffset) {
               for (int y : yOffset) {
                  for (int z : zOffset) {
                     if (this.getBlock(pos.offset(x, y, z)) == Blocks.FIRE) {
                        return;
                     }
                  }
               }
            }

            for (int x : xOffset) {
               for (int y : yOffset) {
                  for (int zx : zOffset) {
                     if (canFire(pos.offset(x, y, zx))) {
                        this.doSwap(fire);
                        this.placeFire(pos.offset(x, y, zx));
                        if (this.inventory.getValue()) {
                           this.doSwap(fire);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public void placeFire(BlockPos pos) {
      BlockPos neighbour = pos.relative(Direction.DOWN);
      BlockUtil.clickBlock(neighbour, Direction.UP, this.rotate.getValue());
   }

   private void doRedStone(BlockPos pos, Direction facing, BlockPos crystalPos) {
      if (mc.level.isEmptyBlock(pos.relative(facing, -1))
         || this.getBlock(pos.relative(facing, -1)) == Blocks.FIRE
         || this.getBlock(pos.relative(facing.getOpposite())) == Blocks.MOVING_PISTON) {
         for (Direction i : Direction.values()) {
            if (this.getBlock(pos.relative(i)) == Blocks.REDSTONE_BLOCK) {
               return;
            }
         }

         int power = this.findBlock(Blocks.REDSTONE_BLOCK);
         if (power != -1) {
            int old = mc.player.getInventory().selected;
            Direction bestNeighboring = BlockUtil.getBestNeighboring(pos, facing);
            if (bestNeighboring != null
               && bestNeighboring != facing.getOpposite()
               && BlockUtil.canPlace(pos.relative(bestNeighboring), this.placeRange.getValue())
               && !pos.relative(bestNeighboring).equals(crystalPos)) {
               this.doSwap(power);
               BlockUtil.placeBlock(pos.relative(bestNeighboring), this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(power);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(old);
               }
            } else {
               for (Direction ix : Direction.values()) {
                  if (BlockUtil.canPlace(pos.relative(ix), this.placeRange.getValue()) && !pos.relative(ix).equals(crystalPos) && ix != facing.getOpposite()) {
                     this.doSwap(power);
                     BlockUtil.placeBlock(pos.relative(ix), this.rotate.getValue());
                     if (this.inventory.getValue()) {
                        this.doSwap(power);
                        EntityUtil.syncInventory();
                     } else {
                        this.doSwap(old);
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selected);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   public int findItem(Item itemIn) {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(itemIn) : InventoryUtil.findItem(itemIn);
   }

   public int findBlock(Block blockIn) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(blockIn) : InventoryUtil.findBlock(blockIn);
   }

   public int findClass(Class<?> clazz) {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(clazz) : InventoryUtil.findClass(clazz);
   }

   private Block getBlock(BlockPos pos) {
      return mc.level.getBlockState(pos).getBlock();
   }
}
