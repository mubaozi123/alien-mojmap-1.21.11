package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class CevBreaker extends Module {
   public static CevBreaker INSTANCE;
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 0.0, 8.0, 0.1));
   private final SliderSetting breakRange = this.add(new SliderSetting("BreakRange", 5.0, 0.0, 8.0, 0.1));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting ground = this.add(new BooleanSetting("Ground", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting top = this.add(new BooleanSetting("Top", false));
   private final BooleanSetting bevel = this.add(new BooleanSetting("Bevel", true));
   private final Timer timer = new Timer();
   private Player target = null;

   public CevBreaker() {
      super("CevBreaker", Module.Category.Combat);
      this.setChinese("自动炸头");
      INSTANCE = this;
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      return mc.level.isEmptyBlock(pos)
         && BlockUtil.noEntityBlockCrystal(pos, false)
         && BlockUtil.noEntityBlockCrystal(pos.above(), false)
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.level.isEmptyBlock(pos.above()));
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (!this.ground.getValue() || mc.player.onGround()) {
            PacketMine.INSTANCE.crystal.setValue(true);
            if ((this.target = CombatUtil.getClosestEnemy(this.targetRange.getValue())) != null) {
               BlockPos targetPos = EntityUtil.getEntityPos(this.target);
               if (PacketMine.getBreakPos() != null) {
                  for (Direction facing : Direction.values()) {
                     if (facing != Direction.DOWN && (facing == Direction.UP ? this.top.getValue() : this.bevel.getValue())) {
                        BlockPos pos = targetPos.above(1).relative(facing);
                        if (!(pos.above().getCenter().distanceTo(mc.player.position()) > this.breakRange.getValue())
                           && PacketMine.getBreakPos().equals(targetPos.above(1).relative(facing))) {
                           if (canPlaceCrystal(targetPos.above(2).relative(facing))) {
                              if (mc.level.isEmptyBlock(pos)) {
                                 if (BlockUtil.canPlace(pos)) {
                                    if (!this.timer.passedMs(this.delay.getValue())) {
                                       return;
                                    }

                                    this.placeBlock(pos);
                                    this.timer.reset();
                                    return;
                                 }
                              } else if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                                 PacketMine.INSTANCE.mine(pos);
                                 this.timer.reset();
                                 return;
                              }
                           } else if (BlockUtil.hasCrystal(targetPos.above(2).relative(facing))) {
                              if (mc.level.isEmptyBlock(pos)) {
                                 return;
                              }

                              if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                                 PacketMine.INSTANCE.mine(pos);
                                 this.timer.reset();
                                 return;
                              }
                           }
                        }
                     }
                  }
               }

               for (Direction facingx : Direction.values()) {
                  if (facingx != Direction.DOWN && (facingx == Direction.UP ? this.top.getValue() : this.bevel.getValue())) {
                     BlockPos pos = targetPos.above(1).relative(facingx);
                     if (!(pos.above().getCenter().distanceTo(mc.player.position()) > this.breakRange.getValue())) {
                        if (canPlaceCrystal(targetPos.above(2).relative(facingx))) {
                           if (mc.level.isEmptyBlock(pos)) {
                              if (BlockUtil.canPlace(pos)) {
                                 if (!this.timer.passedMs(this.delay.getValue())) {
                                    return;
                                 }

                                 this.placeBlock(pos);
                                 this.timer.reset();
                                 break;
                              }
                           } else if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                              PacketMine.INSTANCE.mine(pos);
                              this.timer.reset();
                              break;
                           }
                        } else if (BlockUtil.hasCrystal(targetPos.above(2).relative(facingx))) {
                           if (mc.level.isEmptyBlock(pos)) {
                              break;
                           }

                           if (this.getBlock(pos) == Blocks.OBSIDIAN) {
                              PacketMine.INSTANCE.mine(pos);
                              this.timer.reset();
                              break;
                           }
                        }
                     }
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

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) : InventoryUtil.findBlock(Blocks.OBSIDIAN);
   }

   private void placeBlock(BlockPos pos) {
      int block;
      if ((block = this.getBlock()) != -1) {
         int oldSlot = mc.player.getInventory().selected;
         if (BlockUtil.canPlace(pos)) {
            if (BlockUtil.allowAirPlace()) {
               this.doSwap(block);
               BlockUtil.placedPos.add(pos);
               BlockUtil.airPlace(pos, this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(oldSlot);
               }

               return;
            }

            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            this.doSwap(block);
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.relative(side), side.getOpposite(), this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private Block getBlock(BlockPos pos) {
      return mc.level.getBlockState(pos).getBlock();
   }
}
