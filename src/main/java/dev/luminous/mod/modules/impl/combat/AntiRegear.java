package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaceBlockEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class AntiRegear extends Module {
   public static AntiRegear INSTANCE;
   public final List<BlockPos> safe = new ArrayList<>();
   private final SliderSetting safeRange = this.add(new SliderSetting("SafeRange", 2.0, 0.0, 8.0, 0.1));
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 8.0, 0.1));
   private final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", true));

   public AntiRegear() {
      super("AntiRegear", Module.Category.Combat);
      this.setChinese("反补给");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || !(mc.level.getBlockState(PacketMine.getBreakPos()).getBlock() instanceof ShulkerBoxBlock)) {
         this.safe.removeIf(pos -> !(mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
         if (this.getBlock() != null) {
            PacketMine.INSTANCE.mine(this.getBlock().getBlockPos());
         }
      }
   }

   @EventListener
   public void onPlace(PlaceBlockEvent event) {
      if (event.block instanceof ShulkerBoxBlock) {
         this.safe.add(event.blockPos);
      }
   }

   private ShulkerBoxBlockEntity getBlock() {
      for (BlockEntity entity : BlockUtil.getTileEntities()) {
         if (entity instanceof ShulkerBoxBlockEntity shulker
            && !(Mth.sqrt((float)mc.player.distanceToSqr(shulker.getBlockPos().getCenter())) <= this.safeRange.getValue())
            && (
               !this.checkSelf.getValue()
                  || !this.safe.contains(shulker.getBlockPos())
                     && (!shulker.getBlockPos().equals(AutoRegear.INSTANCE.placePos) || AutoRegear.INSTANCE.timeoutTimer.passed(100L))
            )
            && Mth.sqrt((float)mc.player.distanceToSqr(shulker.getBlockPos().getCenter())) <= this.range.getValue()) {
            return shulker;
         }
      }

      return null;
   }
}
