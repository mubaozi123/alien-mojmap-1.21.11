package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoTrap extends Module {
   public static AutoTrap INSTANCE;
   public final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 500).setSuffix("ms"));
   private final EnumSetting<AutoTrap.TargetMode> targetMod = this.add(new EnumSetting<>("TargetMode", AutoTrap.TargetMode.Single));
   private final EnumSetting<AutoTrap.Mode> headMode = this.add(new EnumSetting<>("BlockForHead", AutoTrap.Mode.Anchor));
   final ArrayList<BlockPos> trapList = new ArrayList<>();
   final ArrayList<BlockPos> placeList = new ArrayList<>();
   private final Timer timer = new Timer();
   private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.0, 1.0, 6.0).setSuffix("m"));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8));
   public final SliderSetting predictTicks = this.add(new SliderSetting("PredictTicks", 2.0, 0.0, 50.0, 1.0));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0).setSuffix("m"));
   private final BooleanSetting checkMine = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting helper = this.add(new BooleanSetting("Helper", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting onlyCrawling = this.add(new BooleanSetting("OnlyCrawling", false));
   private final BooleanSetting checkElytra = this.add(new BooleanSetting("CheckElytra", false));
   private final BooleanSetting extend = this.add(new BooleanSetting("Extend", true));
   private final BooleanSetting antiStep = this.add(new BooleanSetting("AntiStep", false));
   private final BooleanSetting onlyBreak = this.add(new BooleanSetting("OnlyBreak", false, this.antiStep::getValue));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting headExtend = this.add(new BooleanSetting("HeadExtend", true));
   private final BooleanSetting chestUp = this.add(new BooleanSetting("ChestUp", true));
   private final BooleanSetting onlyBreaking = this.add(new BooleanSetting("OnlyBreaking", false, this.chestUp::getValue));
   private final BooleanSetting chest = this.add(new BooleanSetting("Chest", true));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", false, this.chest::getValue));
   private final BooleanSetting ignoreCrawling = this.add(new BooleanSetting("IgnoreCrawling", false, this.chest::getValue));
   private final BooleanSetting legs = this.add(new BooleanSetting("Legs", false));
   private final BooleanSetting legAnchor = this.add(new BooleanSetting("LegAnchor", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting onlyHole = this.add(new BooleanSetting("OnlyHole", false));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final BooleanSetting selfGround = this.add(new BooleanSetting("SelfGround", true));
   public Player target;
   int progress = 0;

   public AutoTrap() {
      super("AutoTrap", Module.Category.Combat);
      this.setChinese("自动困住");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.trapList.clear();
      this.placeList.clear();
      this.progress = 0;
      this.target = null;
      if (!this.selfGround.getValue() || mc.player.onGround()) {
         if (!this.inventory.getValue() || EntityUtil.inInventory()) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  if (this.timer.passed((long)this.delay.getValue())) {
                     if (this.targetMod.getValue() == AutoTrap.TargetMode.Single) {
                        this.target = CombatUtil.getClosestEnemy(this.range.getValue());
                        if (this.target == null) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           return;
                        }

                        this.trapTarget(this.target);
                     } else if (this.targetMod.getValue() == AutoTrap.TargetMode.Multi) {
                        boolean found = false;

                        for (Player player : CombatUtil.getEnemies(this.range.getValue())) {
                           found = true;
                           this.target = player;
                           this.trapTarget(this.target);
                        }

                        if (!found) {
                           if (this.autoDisable.getValue()) {
                              this.disable();
                           }

                           this.target = null;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void trapTarget(Player target) {
      if (!this.onlyHole.getValue() || Alien.HOLE.isHole(EntityUtil.getEntityPos(target))) {
         if (!this.onlyCrawling.getValue()
            || target.isVisuallyCrawling()
            || this.checkElytra.getValue()
               && target.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
               && (!(mc.player.getY() < target.getY() + 1.0) || target.isFallFlying())) {
            Vec3 playerPos = this.predictTicks.getValue() > 0.0 ? PredictUtil.getPos(target, this.predictTicks.getValueInt()) : target.position();
            this.doTrap(target, new BlockPosX(playerPos.x(), playerPos.y(), playerPos.z()));
         }
      }
   }

   private void doTrap(Player player, BlockPos pos) {
      if (pos != null) {
         if (!this.trapList.contains(pos)) {
            this.trapList.add(pos);
            int headOffset = player.isVisuallyCrawling() ? 1 : 2;
            int chestOffset = player.isVisuallyCrawling() ? 0 : 1;
            if (this.legs.getValue()) {
               for (Direction i : Direction.values()) {
                  if (i != Direction.DOWN && i != Direction.UP) {
                     BlockPos offsetPos = pos.relative(i);
                     this.tryPlaceBlock(offsetPos, this.legAnchor.getValue(), false, false);
                     if (BlockUtil.getPlaceSide(offsetPos) == null
                        && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                        && this.getHelper(offsetPos) != null) {
                        this.tryPlaceObsidian(this.getHelper(offsetPos));
                     }
                  }
               }
            }

            if (this.headExtend.getValue()) {
               for (int x : new int[]{1, 0, -1}) {
                  for (int z : new int[]{1, 0, -1}) {
                     BlockPos offsetPos = pos.offset(z, 0, x);
                     if (this.checkEntity(new BlockPos(offsetPos))) {
                        this.tryPlaceBlock(
                           offsetPos.above(headOffset),
                           this.headMode.getValue() == AutoTrap.Mode.Anchor,
                           this.headMode.getValue() == AutoTrap.Mode.Concrete,
                           this.headMode.getValue() == AutoTrap.Mode.Web
                        );
                     }
                  }
               }
            }

            if (this.head.getValue() && BlockUtil.clientCanPlace(pos.above(headOffset), this.breakCrystal.getValue())) {
               if (BlockUtil.getPlaceSide(pos.above(headOffset)) == null) {
                  boolean trapChest = this.helper.getValue();
                  if (this.getHelper(pos.above(headOffset)) != null) {
                     this.tryPlaceObsidian(this.getHelper(pos.above(headOffset)));
                     trapChest = false;
                  }

                  if (trapChest) {
                     for (Direction ix : Direction.values()) {
                        if (ix != Direction.DOWN && ix != Direction.UP) {
                           BlockPos offsetPos = pos.relative(ix).above(chestOffset);
                           if (BlockUtil.isStrictDirection(pos.relative(ix).above(), ix.getOpposite())
                              && BlockUtil.clientCanPlace(offsetPos.above(chestOffset), this.breakCrystal.getValue())
                              && BlockUtil.canPlace(offsetPos, this.placeRange.getValue(), this.breakCrystal.getValue())) {
                              this.tryPlaceObsidian(offsetPos);
                              trapChest = false;
                              break;
                           }
                        }
                     }

                     if (trapChest) {
                        for (Direction ixx : Direction.values()) {
                           if (ixx != Direction.DOWN && ixx != Direction.UP) {
                              BlockPos offsetPos = pos.relative(ixx).above(chestOffset);
                              if (BlockUtil.isStrictDirection(pos.relative(ixx).above(), ixx.getOpposite())
                                 && BlockUtil.clientCanPlace(offsetPos.above(chestOffset), this.breakCrystal.getValue())
                                 && BlockUtil.getPlaceSide(offsetPos) == null
                                 && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                 && this.getHelper(offsetPos) != null) {
                                 this.tryPlaceObsidian(this.getHelper(offsetPos));
                                 trapChest = false;
                                 break;
                              }
                           }
                        }

                        if (trapChest) {
                           for (Direction ixxx : Direction.values()) {
                              if (ixxx != Direction.DOWN && ixxx != Direction.UP) {
                                 BlockPos offsetPos = pos.relative(ixxx).above(chestOffset);
                                 if (BlockUtil.isStrictDirection(pos.relative(ixxx).above(), ixxx.getOpposite())
                                    && BlockUtil.clientCanPlace(offsetPos.above(chestOffset), this.breakCrystal.getValue())
                                    && BlockUtil.getPlaceSide(offsetPos) == null
                                    && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                                    && this.getHelper(offsetPos) != null
                                    && BlockUtil.getPlaceSide(offsetPos.below()) == null
                                    && BlockUtil.clientCanPlace(offsetPos.below(), this.breakCrystal.getValue())
                                    && this.getHelper(offsetPos.below()) != null) {
                                    this.tryPlaceObsidian(this.getHelper(offsetPos.below()));
                                    break;
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               this.tryPlaceBlock(
                  pos.above(headOffset),
                  this.headMode.getValue() == AutoTrap.Mode.Anchor,
                  this.headMode.getValue() == AutoTrap.Mode.Concrete,
                  this.headMode.getValue() == AutoTrap.Mode.Web
               );
            }

            if (this.antiStep.getValue() && (Alien.BREAK.isMining(pos.above(headOffset)) || !this.onlyBreak.getValue())) {
               if (BlockUtil.getPlaceSide(pos.above(3)) == null
                  && BlockUtil.clientCanPlace(pos.above(3), this.breakCrystal.getValue())
                  && this.getHelper(pos.above(3), Direction.DOWN) != null) {
                  this.tryPlaceObsidian(this.getHelper(pos.above(3)));
               }

               this.tryPlaceObsidian(pos.above(3));
            }

            if (this.down.getValue()) {
               BlockPos offsetPos = pos.below();
               this.tryPlaceObsidian(offsetPos);
               if (BlockUtil.getPlaceSide(offsetPos) == null
                  && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())
                  && this.getHelper(offsetPos) != null) {
                  this.tryPlaceObsidian(this.getHelper(offsetPos));
               }
            }

            if (this.chestUp.getValue()) {
               for (Direction ixxxx : Direction.values()) {
                  if (ixxxx != Direction.DOWN && ixxxx != Direction.UP) {
                     BlockPos offsetPos = pos.relative(ixxxx).above(headOffset);
                     if (!this.onlyBreaking.getValue() || Alien.BREAK.isMining(pos.above(headOffset))) {
                        this.tryPlaceObsidian(offsetPos);
                        if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                           if (this.getHelper(offsetPos) != null) {
                              this.tryPlaceObsidian(this.getHelper(offsetPos));
                           } else if (BlockUtil.getPlaceSide(offsetPos.below()) == null
                              && BlockUtil.clientCanPlace(offsetPos.below(), this.breakCrystal.getValue())
                              && this.getHelper(offsetPos.below()) != null) {
                              this.tryPlaceObsidian(this.getHelper(offsetPos.below()));
                           }
                        }
                     }
                  }
               }
            }

            if (this.chest.getValue()
               && (!this.onlyGround.getValue() || this.target.onGround())
               && (!this.ignoreCrawling.getValue() || !this.target.isVisuallyCrawling())) {
               for (Direction ixxxxx : Direction.values()) {
                  if (ixxxxx != Direction.DOWN && ixxxxx != Direction.UP) {
                     BlockPos offsetPos = pos.relative(ixxxxx).above(chestOffset);
                     this.tryPlaceObsidian(offsetPos);
                     if (BlockUtil.getPlaceSide(offsetPos) == null && BlockUtil.clientCanPlace(offsetPos, this.breakCrystal.getValue())) {
                        if (this.getHelper(offsetPos) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos));
                        } else if (BlockUtil.getPlaceSide(offsetPos.below()) == null
                           && BlockUtil.clientCanPlace(offsetPos.below(), this.breakCrystal.getValue())
                           && this.getHelper(offsetPos.below()) != null) {
                           this.tryPlaceObsidian(this.getHelper(offsetPos.below()));
                        }
                     }
                  }
               }
            }

            if (this.extend.getValue()) {
               for (int x : new int[]{1, 0, -1}) {
                  for (int zx : new int[]{1, 0, -1}) {
                     BlockPos offsetPos = pos.offset(x, 0, zx);
                     if (this.checkEntity(new BlockPos(offsetPos))) {
                        this.doTrap(player, offsetPos);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   public BlockPos getHelper(BlockPos pos) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if ((!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.relative(i)))
               && BlockUtil.isStrictDirection(pos.relative(i), i.getOpposite())
               && BlockUtil.canPlace(pos.relative(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.relative(i);
            }
         }

         return null;
      }
   }

   public BlockPos getHelper(BlockPos pos, Direction ignore) {
      if (!this.helper.getValue()) {
         return null;
      } else {
         for (Direction i : Direction.values()) {
            if (i != ignore
               && (!this.checkMine.getValue() || !Alien.BREAK.isMining(pos.relative(i)))
               && BlockUtil.isStrictDirection(pos.relative(i), i.getOpposite())
               && BlockUtil.canPlace(pos.relative(i), this.placeRange.getValue(), this.breakCrystal.getValue())) {
               return pos.relative(i);
            }
         }

         return null;
      }
   }

   private boolean checkEntity(BlockPos pos) {
      if (mc.player.getBoundingBox().intersects(new AABB(pos))) {
         return false;
      } else {
         for (Entity entity : Alien.THREAD.getPlayers()) {
            if (entity.getBoundingBox().intersects(new AABB(pos)) && entity.isAlive()) {
               return true;
            }
         }

         return false;
      }
   }

   private void tryPlaceBlock(BlockPos pos, boolean anchor, boolean sand, boolean web) {
      if (!this.placeList.contains(pos)) {
         if (!Alien.BREAK.isMining(pos)) {
            if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
               if (this.progress < this.blocksPer.getValue()) {
                  if (!(Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter())) > this.placeRange.getValue())) {
                     int old = mc.player.getInventory().selected;
                     int block = sand
                        ? this.getConcrete()
                        : (
                           web
                              ? (this.getWeb() != -1 ? this.getWeb() : this.getBlock())
                              : (anchor && this.getAnchor() != -1 ? this.getAnchor() : this.getBlock())
                        );
                     if (block != -1) {
                        this.placeList.add(pos);
                        CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                        this.doSwap(block);
                        BlockUtil.placeBlock(pos, this.rotate.getValue());
                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        this.timer.reset();
                        this.progress++;
                     }
                  }
               }
            }
         }
      }
   }

   private void tryPlaceObsidian(BlockPos pos) {
      if (pos != null) {
         if (!this.placeList.contains(pos)) {
            if (!Alien.BREAK.isMining(pos)) {
               if (BlockUtil.canPlace(pos, 6.0, this.breakCrystal.getValue())) {
                  if (this.progress < this.blocksPer.getValue()) {
                     if (!(Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter())) > this.placeRange.getValue())) {
                        int old = mc.player.getInventory().selected;
                        int block = this.getBlock();
                        if (block != -1) {
                           BlockUtil.placedPos.add(pos);
                           this.placeList.add(pos);
                           CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPause.getValue());
                           this.doSwap(block);
                           BlockUtil.placeBlock(pos, this.rotate.getValue());
                           if (this.inventory.getValue()) {
                              this.doSwap(block);
                              EntityUtil.syncInventory();
                           } else {
                              this.doSwap(old);
                           }

                           this.timer.reset();
                           this.progress++;
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

   private int getConcrete() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ConcretePowderBlock.class) : InventoryUtil.findClass(ConcretePowderBlock.class);
   }

   private int getWeb() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
   }

   private int getAnchor() {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.RESPAWN_ANCHOR) : InventoryUtil.findBlock(Blocks.RESPAWN_ANCHOR);
   }

   private static enum Mode {
      Obsidian,
      Anchor,
      Web,
      Concrete;
   }

   public static enum TargetMode {
      Single,
      Multi;
   }
}
