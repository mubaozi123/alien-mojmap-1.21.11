package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class Breaker extends Module {
   public static Breaker INSTANCE;
   public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting face = this.add(new BooleanSetting("Face", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
   private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
   private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );

   public Breaker() {
      super("Breaker", Module.Category.Combat);
      this.setChinese("自动挖掘");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!CevBreaker.INSTANCE.isOn() || !this.cevPause.getValue()) {
         if (!AntiCrawl.INSTANCE.work) {
            if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
               Player player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
               if (player != null) {
                  this.doBreak(player);
               }
            }
         }
      }
   }

   private void doBreak(Player player) {
      BlockPos pos = EntityUtil.getEntityPos(player, true);
      if (PacketMine.getBreakPos() == null
         || PacketMine.getBreakPos().equals(PacketMine.secondPos)
         || PacketMine.secondPos == null
         || mc.level.isEmptyBlock(PacketMine.secondPos)
         || !this.forceDouble.getValue()) {
         double[] yOffset = new double[]{-0.8, 0.3, 1.1};
         double[] xzOffset = new double[]{0.3, -0.3};

         for (Player entity : CombatUtil.getEnemies(this.targetRange.getValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.canBreak(offsetPos) && offsetPos.equals(PacketMine.getBreakPos())) {
                        return;
                     }
                  }
               }
            }
         }

         List<Float> yList = new ArrayList<>();
         if (this.down.getValue()) {
            yList.add(-0.8F);
         }

         if (this.head.getValue()) {
            yList.add(2.3F);
         }

         if (this.burrow.getValue()) {
            yList.add(0.3F);
         }

         if (this.face.getValue()) {
            yList.add(1.1F);
         }

         Iterator var32 = yList.iterator();

         while (var32.hasNext()) {
            double y = ((Float)var32.next()).floatValue();

            for (double offset : xzOffset) {
               BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
               if (this.canBreak(offsetPos)) {
                  PacketMine.INSTANCE.mine(offsetPos);
                  return;
               }
            }
         }

         var32 = yList.iterator();

         while (var32.hasNext()) {
            double y = ((Float)var32.next()).floatValue();

            for (double offsetx : xzOffset) {
               for (double offset2 : xzOffset) {
                  BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offsetx);
                  if (this.canBreak(offsetPos)) {
                     PacketMine.INSTANCE.mine(offsetPos);
                     return;
                  }
               }
            }
         }

         if (this.surround.getValue()) {
            for (Direction i : Direction.values()) {
               if (i != Direction.UP
                  && i != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > this.range.getValue())
                  && (mc.level.isEmptyBlock(pos.relative(i)) || pos.relative(i).equals(PacketMine.getBreakPos()))
                  && this.canPlaceCrystal(pos.relative(i), false)
                  && !pos.relative(i).equals(PacketMine.secondPos)) {
                  return;
               }
            }

            ArrayList<BlockPos> list = new ArrayList<>();

            for (Direction ix : Direction.values()) {
               if (ix != Direction.UP
                  && ix != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(ix).getCenter())) > this.range.getValue())
                  && this.canBreak(pos.relative(ix))
                  && this.canPlaceCrystal(pos.relative(ix), true)
                  && !this.isSurroundPos(pos.relative(ix))) {
                  list.add(pos.relative(ix));
               }
            }

            if (!list.isEmpty()) {
               PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.distToCenterSqr(mc.player.getEyePosition()))).get());
            } else {
               for (Direction ixx : Direction.values()) {
                  if (ixx != Direction.UP
                     && ixx != Direction.DOWN
                     && !(Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(ixx).getCenter())) > this.range.getValue())
                     && this.canBreak(pos.relative(ixx))
                     && this.canPlaceCrystal(pos.relative(ixx), false)) {
                     list.add(pos.relative(ixx));
                  }
               }

               if (!list.isEmpty()) {
                  PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble(E -> E.distToCenterSqr(mc.player.getEyePosition()))).get());
               }
            }
         }
      }
   }

   private boolean isSurroundPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN) {
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (self.relative(i).equals(pos)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.below();
      BlockPos boost = obsPos.above();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
         && BlockUtil.noEntityBlockCrystal(boost, true, true)
         && BlockUtil.noEntityBlockCrystal(boost.above(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      return mc.player.getEyePosition().distanceTo(pos.getCenter()) <= PacketMine.INSTANCE.range.getValue()
         && hard.contains(BlockUtil.getBlock(pos))
         && BlockUtil.getClickSideStrict(pos) != null;
   }

   private boolean canBreak(BlockPos pos) {
      return this.isObsidian(pos)
         && (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos()))
         && (
            !pos.equals(PacketMine.secondPos)
               || !mc.player.getMainHandItem().is(ItemTags.PICKAXES)
                  && !PacketMine.INSTANCE.autoSwitch.getValue()
                  && !PacketMine.INSTANCE.noGhostHand.getValue()
         );
   }
}
