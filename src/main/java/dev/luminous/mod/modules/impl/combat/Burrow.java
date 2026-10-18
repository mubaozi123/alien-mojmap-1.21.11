package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Burrow extends Module {
   public static Burrow INSTANCE;
   private final EnumSetting<Burrow.RotateMode> rotate = this.add(new EnumSetting<>("RotateMode", Burrow.RotateMode.Bypass));
   private final EnumSetting<Burrow.LagBackMode> lagMode = this.add(new EnumSetting<>("LagMode", Burrow.LagBackMode.TrollHack));
   private final EnumSetting<Burrow.LagBackMode> aboveLagMode = this.add(new EnumSetting<>("MoveLagMode", Burrow.LagBackMode.Smart));
   private final List<BlockPos> placePos = new ArrayList<>();
   private final Timer tnmer = new Timer();
   private final Timer webTimer = new Timer();
   private final BooleanSetting disable = this.add(new BooleanSetting("Disable", true));
   private final BooleanSetting jumpDisable = this.add(new BooleanSetting("JumpDisable", true, () -> !this.disable.getValue()));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 500, 0, 1000, () -> !this.disable.getValue()));
   private final SliderSetting webTime = this.add(new SliderSetting("WebTime", 0, 0, 500));
   private final BooleanSetting enderChest = this.add(new BooleanSetting("EnderChest", true));
   private final BooleanSetting antiLag = this.add(new BooleanSetting("AntiLag", false));
   private final BooleanSetting single = this.add(new BooleanSetting("SingleBlock", false));
   private final BooleanSetting detectMine = this.add(new BooleanSetting("DetectMining", false));
   private final BooleanSetting headFill = this.add(new BooleanSetting("HeadFill", false));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", true));
   private final BooleanSetting noSelfPos = this.add(new BooleanSetting("NoSelfPos", false));
   private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
   private final BooleanSetting sound = this.add(new BooleanSetting("Sound", true));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 4.0, 1.0, 4.0, 1.0));
   private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true));
   private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, this.disable::getValue));
   private final BooleanSetting fakeMove = this.add(new BooleanSetting("FakeMove", true).setParent());
   private final BooleanSetting center = this.add(new BooleanSetting("AllowCenter", true, this.fakeMove::isOpen));
   private final SliderSetting preCorrect = this.add(new SliderSetting("PreCorrect", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveDis = this.add(new SliderSetting("MoveDis", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveDis2 = this.add(new SliderSetting("MoveDis2", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting moveCorrect2 = this.add(new SliderSetting("Correct", 0.25, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0.01, 0.0, 1.0, 0.001, this.fakeMove::isOpen));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final SliderSetting smartX = this.add(
      new SliderSetting(
         "SmartXZ", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartUp = this.add(
      new SliderSetting(
         "SmartUp", 3.0, 0.0, 10.0, 0.1, () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartDown = this.add(
      new SliderSetting(
         "SmartDown",
         3.0,
         0.0,
         10.0,
         0.1,
         () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private final SliderSetting smartDistance = this.add(
      new SliderSetting(
         "SmartDistance",
         2.0,
         0.0,
         10.0,
         0.1,
         () -> this.lagMode.getValue() == Burrow.LagBackMode.Smart || this.aboveLagMode.getValue() == Burrow.LagBackMode.Smart
      )
   );
   private int progress = 0;
   private Vec3 currentPos;

   public Burrow() {
      super("Burrow", Module.Category.Combat);
      this.setChinese("卡黑曜石");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(ClientTickEvent event) {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (event.isPost()) {
            if (!this.disable.getValue() && this.jumpDisable.getValue() && mc.player.input.keyPresses.jump()) {
               this.disable();
               return;
            }

            if (Alien.PLAYER.isInWeb(mc.player)) {
               this.webTimer.reset();
               return;
            }

            if (this.usingPause.getValue() && mc.player.isUsingItem()) {
               return;
            }

            if (!this.webTimer.passedMs(this.webTime.getValue())) {
               return;
            }

            if (!this.disable.getValue() && !this.tnmer.passedMs(this.delay.getValue())) {
               return;
            }

            if (!mc.player.onGround()) {
               return;
            }

            if (this.antiLag.getValue() && !BlockUtil.canCollide(mc.player, new AABB(EntityUtil.getPlayerPos(true).below()))) {
               return;
            }

            if (this.single.getValue() && EntityUtil.isInsideBlock()) {
               if (this.disable.getValue()) {
                  this.disable();
               }

               return;
            }

            if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
               return;
            }

            int oldSlot = mc.player.getInventory().selected;
            int block = this.getBlock();
            if (block == -1) {
               CommandManager.sendMessageId("§4No block found.", this.hashCode() - 1);
               this.disable();
               return;
            }

            this.progress = 0;
            this.placePos.clear();
            double offset = this.single.getValue() ? 0.0 : AntiCheat.getOffset();
            BlockPosX pos1 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
            BlockPosX pos2 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() + offset);
            BlockPosX pos3 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
            BlockPosX pos4 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 0.5, mc.player.getZ() - offset);
            BlockPosX pos5 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
            BlockPosX pos6 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() + offset);
            BlockPosX pos7 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
            BlockPosX pos8 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() + 1.5, mc.player.getZ() - offset);
            BlockPosX pos9 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1.0, mc.player.getZ() + offset);
            BlockPosX pos10 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1.0, mc.player.getZ() + offset);
            BlockPosX pos11 = new BlockPosX(mc.player.getX() + offset, mc.player.getY() - 1.0, mc.player.getZ() - offset);
            BlockPosX pos12 = new BlockPosX(mc.player.getX() - offset, mc.player.getY() - 1.0, mc.player.getZ() - offset);
            BlockPos playerPos = EntityUtil.getPlayerPos(true);
            boolean headFill = false;
            if (!this.canPlace(pos1) && !this.canPlace(pos2) && !this.canPlace(pos3) && !this.canPlace(pos4)) {
               boolean cantHeadFill = !this.headFill.getValue() || !this.canPlace(pos5) && !this.canPlace(pos6) && !this.canPlace(pos7) && !this.canPlace(pos8);
               boolean cantDown = !this.down.getValue() || !this.canPlace(pos9) && !this.canPlace(pos10) && !this.canPlace(pos11) && !this.canPlace(pos12);
               if (cantHeadFill) {
                  if (cantDown) {
                     if (!this.wait.getValue() && this.disable.getValue()) {
                        this.disable();
                     }

                     return;
                  }
               } else {
                  headFill = true;
               }
            }

            boolean above = false;
            BlockPos headPos = EntityUtil.getPlayerPos(true).above(2);
            boolean rotate = this.rotate.getValue() == Burrow.RotateMode.Normal;
            CombatUtil.attackCrystal(pos1, rotate, false);
            CombatUtil.attackCrystal(pos2, rotate, false);
            CombatUtil.attackCrystal(pos3, rotate, false);
            CombatUtil.attackCrystal(pos4, rotate, false);
            if (!headFill
               && !mc.player.isVisuallyCrawling()
               && !this.trapped(headPos)
               && !this.trapped(headPos.offset(1, 0, 0))
               && !this.trapped(headPos.offset(-1, 0, 0))
               && !this.trapped(headPos.offset(0, 0, 1))
               && !this.trapped(headPos.offset(0, 0, -1))
               && !this.trapped(headPos.offset(1, 0, -1))
               && !this.trapped(headPos.offset(-1, 0, -1))
               && !this.trapped(headPos.offset(1, 0, 1))
               && !this.trapped(headPos.offset(-1, 0, 1))) {
               mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false, false));
               mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false, false));
               mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.9999957640154541, mc.player.getZ(), false, false));
               mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ(), false, false));
               this.currentPos = new Vec3(mc.player.getX(), mc.player.getY() + 1.1661092609382138, mc.player.getZ());
            } else {
               above = true;
               if (!this.fakeMove.getValue()) {
                  if (!this.wait.getValue() && this.disable.getValue()) {
                     this.disable();
                  }

                  return;
               }

               if (!this.fakeMove()) {
                  return;
               }
            }

            this.tnmer.reset();
            this.doSwap(block);
            if (this.rotate.getValue() == Burrow.RotateMode.Bypass) {
               if (above) {
                  float[] angle = RotationManager.getRotation(
                     this.currentPos.add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0), mc.player.getEyePosition(1.0F)
                  );
                  Alien.ROTATION.snapAt(angle[0], angle[1]);
               } else {
                  Alien.ROTATION.snapAt(Alien.ROTATION.rotationYaw, 90.0F);
               }
            }

            this.placeBlock(playerPos, rotate);
            this.placeBlock(pos1, rotate);
            this.placeBlock(pos2, rotate);
            this.placeBlock(pos3, rotate);
            this.placeBlock(pos4, rotate);
            if (this.down.getValue()) {
               this.placeBlock(pos9, rotate);
               this.placeBlock(pos10, rotate);
               this.placeBlock(pos11, rotate);
               this.placeBlock(pos12, rotate);
            }

            if (this.headFill.getValue() && above) {
               this.placeBlock(pos5, rotate);
               this.placeBlock(pos6, rotate);
               this.placeBlock(pos7, rotate);
               this.placeBlock(pos8, rotate);
            }

            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }

            switch (above ? (Burrow.LagBackMode)this.aboveLagMode.getValue() : (Burrow.LagBackMode)this.lagMode.getValue()) {
               case Smart:
                  ArrayList<BlockPosX> list = new ArrayList<>();

                  for (double x = mc.player.getEyePosition(1.0F).x() - this.smartX.getValue();
                     x < mc.player.getEyePosition(1.0F).x() + this.smartX.getValue();
                     x++
                  ) {
                     for (double z = mc.player.getEyePosition(1.0F).z() - this.smartX.getValue();
                        z < mc.player.getEyePosition(1.0F).z() + this.smartX.getValue();
                        z++
                     ) {
                        for (double d = mc.player.getEyePosition(1.0F).y() - this.smartDown.getValue();
                           d < mc.player.getEyePosition(1.0F).y() + this.smartUp.getValue();
                           d++
                        ) {
                           list.add(new BlockPosX(x, d, z));
                        }
                     }
                  }

                  double getDistance = 0.0;
                  BlockPos bestPos = null;

                  for (BlockPos blockPos : list) {
                     if (this.canMove(blockPos)
                        && !(Mth.sqrt((float)mc.player.distanceToSqr(blockPos.getCenter().add(0.0, -0.5, 0.0))) < this.smartDistance.getValue())
                        && (bestPos == null || mc.player.distanceToSqr(blockPos.getCenter()) < getDistance)) {
                        bestPos = blockPos;
                        getDistance = mc.player.distanceToSqr(blockPos.getCenter());
                     }
                  }

                  if (bestPos != null) {
                     mc.getConnection().send(new Pos(bestPos.getX() + 0.5, bestPos.getY(), bestPos.getZ() + 0.5, false, false));
                  }
                  break;
               case Invalid:
                  for (int n = 0; n < 20; n++) {
                     mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1337.0, mc.player.getZ(), false, false));
                  }
                  break;
               case TrollHack:
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false, false));
                  break;
               case ToVoid:
                  mc.getConnection().send(new Pos(mc.player.getX(), -70.0, mc.player.getZ(), false, false));
                  break;
               case ToVoid2:
                  mc.getConnection().send(new Pos(mc.player.getX(), -7.0, mc.player.getZ(), false, false));
                  break;
               case Normal:
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.9, mc.player.getZ(), false, false));
                  break;
               case Rotation:
                  mc.getConnection().send(new Rot(-180.0F, -90.0F, false, false));
                  mc.getConnection().send(new Rot(180.0F, 90.0F, false, false));
                  break;
               case Fly:
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.170005801788139, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.2426308013947485, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 2.3400880035762786, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 2.640088003576279, mc.player.getZ(), false, false));
                  break;
               case Glide:
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.1001, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.0605, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.0802, mc.player.getZ(), false, false));
                  mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.1127, mc.player.getZ(), false, false));
            }

            if (this.disable.getValue()) {
               this.disable();
            }
         }
      }
   }

   private boolean fakeMove() {
      double[] offsets = new double[]{1.0, 0.0, -1.0};
      ArrayList<BlockPosX> offList = new ArrayList<>();
      BlockPos playerPos = mc.player.blockPosition();

      for (double x : offsets) {
         for (double z : offsets) {
            offList.add(new BlockPosX(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z));
         }
      }

      for (BlockPos offPos : offList) {
         if (this.checkSelf(offPos) && !BlockUtil.canReplace(offPos) && (!this.headFill.getValue() || !BlockUtil.canReplace(offPos.above()))) {
            this.gotoPos(offPos);
            return true;
         }
      }

      ArrayList<BlockPos> pos = new ArrayList<>();

      for (BlockPos blockPos : offList) {
         if (!playerPos.equals(blockPos) && this.checkSelf(blockPos) && this.canMove(blockPos)) {
            pos.add(blockPos);
         }
      }

      if (!pos.isEmpty()) {
         double dis = 10.0;
         BlockPos target = null;

         for (BlockPos p : pos) {
            double getDistance = mc.player.getEyePosition(1.0F).distanceTo(p.getCenter().add(0.0, -0.5, 0.0));
            if (getDistance < dis || target == null) {
               target = p;
               dis = getDistance;
            }
         }

         this.gotoPos(target);
         return true;
      } else {
         for (BlockPos blockPosx : offList) {
            if (!playerPos.equals(blockPosx) && this.checkSelf(blockPosx)) {
               pos.add(blockPosx);
            }
         }

         if (!pos.isEmpty()) {
            double dis = 10.0;
            BlockPos target = null;

            for (BlockPos px : pos) {
               double getDistance = mc.player.getEyePosition(1.0F).distanceTo(px.getCenter().add(0.0, -0.5, 0.0));
               if (getDistance < dis || target == null) {
                  target = px;
                  dis = getDistance;
               }
            }

            this.gotoPos(target);
            return true;
         } else if (!this.center.getValue()) {
            return false;
         } else {
            offList.clear();
            offList.add(new BlockPosX(mc.player.getX() + 1.0, mc.player.getY(), mc.player.getZ()));
            offList.add(new BlockPosX(mc.player.getX() - 1.0, mc.player.getY(), mc.player.getZ()));
            offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() - 1.0));
            offList.add(new BlockPosX(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 1.0));

            for (BlockPos blockPosxx : offList) {
               if (this.canMove(blockPosxx)) {
                  this.gotoPos(blockPosxx);
                  return true;
               }
            }

            if (!this.wait.getValue() && this.disable.getValue()) {
               this.disable();
            }

            return false;
         }
      }
   }

   private void placeBlock(BlockPos pos, boolean rotate) {
      if (this.canPlace(pos) && !this.placePos.contains(pos) && this.progress < this.blocksPer.getValueInt()) {
         this.placePos.add(pos);
         if (BlockUtil.allowAirPlace()) {
            this.progress++;
            BlockUtil.placedPos.add(pos);
            if (this.sound.getValue()) {
               mc.level.playSound(mc.player, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }

            this.clickBlock(pos, Direction.DOWN, rotate, InteractionHand.MAIN_HAND, this.packetPlace.getValue());
         }

         Direction side;
         if ((side = BlockUtil.getPlaceSide(pos)) == null) {
            return;
         }

         this.progress++;
         BlockUtil.placedPos.add(pos);
         if (this.sound.getValue()) {
            mc.level.playSound(mc.player, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
         }

         this.clickBlock(pos.relative(side), side.getOpposite(), rotate, InteractionHand.MAIN_HAND, this.packetPlace.getValue());
      }
   }

   public void clickBlock(BlockPos pos, Direction side, boolean rotate, InteractionHand hand, boolean packet) {
      Vec3 directionVec = new Vec3(
         pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
         pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
         pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
      );
      if (rotate) {
         float[] angle = RotationManager.getRotation(this.currentPos.add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0), directionVec);
         Alien.ROTATION.snapAt(angle[0], angle[1]);
      }

      EntityUtil.swingHand(hand, AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      if (packet) {
         Module.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, result, id));
      } else {
         mc.gameMode.useItemOn(mc.player, hand, result);
      }

      if (rotate) {
         Alien.ROTATION.snapBack();
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selected);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private void gotoPos(BlockPos offPos) {
      double targetX = offPos.getX() + 0.5;
      double targetZ = offPos.getZ() + 0.5;
      double x = mc.player.getX();
      double z = mc.player.getZ();
      double y = mc.player.getY() + this.yOffset.getValue();
      double xDiff = Math.abs(x - targetX);
      double zDiff = Math.abs(z - targetZ);
      double moveDis = this.preCorrect.getValue();
      if (moveDis > 0.0) {
         if (xDiff >= moveDis) {
            double var18;
            double var19;
            x = x > targetX ? (var18 = x - moveDis) : (var19 = x + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
         }

         if (zDiff >= moveDis) {
            double var26;
            double var27;
            z = z > targetZ ? (var26 = z - moveDis) : (var27 = z + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
         }
      }

      xDiff = Math.abs(x - targetX);
      zDiff = Math.abs(z - targetZ);
      moveDis = this.moveDis.getValue();
      if (moveDis > 0.0) {
         while (xDiff > moveDis) {
            double var20;
            double var21;
            x = x > targetX ? (var20 = x - moveDis) : (var21 = x + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
            xDiff = Math.abs(x - targetX);
         }

         while (zDiff > moveDis) {
            double var28;
            double var29;
            z = z > targetZ ? (var28 = z - moveDis) : (var29 = z + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveDis2.getValue()) > 0.0) {
         while (xDiff > moveDis) {
            double var22;
            double var23;
            x = x > targetX ? (var22 = x - moveDis) : (var23 = x + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
            xDiff = Math.abs(x - targetX);
         }

         while (zDiff > moveDis) {
            double var30;
            double var31;
            z = z > targetZ ? (var30 = z - moveDis) : (var31 = z + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
            zDiff = Math.abs(z - targetZ);
         }
      }

      if ((moveDis = this.moveCorrect2.getValue()) > 0.0) {
         if (xDiff >= moveDis) {
            double var24;
            double var25;
            x = x > targetX ? (var24 = x - moveDis) : (var25 = x + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
         }

         if (zDiff >= moveDis) {
            double var32;
            double var33;
            z = z > targetZ ? (var32 = z - moveDis) : (var33 = z + moveDis);
            mc.player.connection.send(new Pos(x, y, z, false, false));
         }
      }

      this.currentPos = new Vec3(x, y, z);
   }

   private boolean canMove(BlockPos pos) {
      return mc.level.isEmptyBlock(pos) && mc.level.isEmptyBlock(pos.above());
   }

   private boolean canPlace(BlockPos pos) {
      if (this.noSelfPos.getValue() && pos.equals(EntityUtil.getPlayerPos(true))) {
         return false;
      } else if (!BlockUtil.allowAirPlace() && BlockUtil.getPlaceSide(pos) == null) {
         return false;
      } else if (!BlockUtil.canReplace(pos)) {
         return false;
      } else {
         return this.detectMine.getValue() && Alien.BREAK.isMining(pos) ? false : !this.hasEntity(pos);
      }
   }

   private boolean hasEntity(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new AABB(pos))) {
         if (entity != mc.player
            && entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrb)
            && !(entity instanceof ThrownExperienceBottle)
            && !(entity instanceof Arrow)
            && (!(entity instanceof EndCrystal) || !this.breakCrystal.getValue())) {
            return true;
         }
      }

      return false;
   }

   private boolean checkSelf(BlockPos pos) {
      return mc.player.getBoundingBox().intersects(new AABB(pos));
   }

   private boolean trapped(BlockPos pos) {
      return (BlockUtil.canCollide(mc.player, new AABB(pos)) || BlockUtil.getBlock(pos) == Blocks.COBWEB) && this.checkSelf(pos.below(2));
   }

   private int getBlock() {
      if (this.inventory.getValue()) {
         return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
      } else {
         return InventoryUtil.findBlock(Blocks.OBSIDIAN) == -1 && this.enderChest.getValue()
            ? InventoryUtil.findBlock(Blocks.ENDER_CHEST)
            : InventoryUtil.findBlock(Blocks.OBSIDIAN);
      }
   }

   private static enum LagBackMode {
      Smart,
      Invalid,
      TrollHack,
      ToVoid,
      ToVoid2,
      Normal,
      Rotation,
      Fly,
      Glide;
   }

   private static enum RotateMode {
      Bypass,
      Normal,
      None;
   }
}
