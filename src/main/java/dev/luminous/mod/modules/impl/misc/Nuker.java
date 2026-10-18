package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.level.block.Blocks;

public class Nuker extends Module {
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting sand = this.add(new BooleanSetting("Sand", false));
   private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, this.sand::getValue));

   public Nuker() {
      super("Nuker", Module.Category.Misc);
      this.setChinese("范围挖掘");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (PacketMine.getBreakPos() == null || mc.level.isEmptyBlock(PacketMine.getBreakPos())) {
         if (this.sand.getValue()) {
            if (!mc.player.onGround()) {
               return;
            }

            int b = 0;

            for (BlockPos sand : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePosition())) {
               if (Blocks.SAND == mc.level.getBlockState(sand).getBlock() || Blocks.RED_SAND == mc.level.getBlockState(sand).getBlock()) {
                  Direction side = BlockUtil.getClickSideStrict(sand);
                  if (side != null) {
                     PlaceRender.INSTANCE.create(sand);
                     Alien.ROTATION.snapAt(sand.getCenter());
                     sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                     sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                     sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                     Alien.ROTATION.snapBack();
                     if (++b >= this.breaks.getValue()) {
                        return;
                     }
                  }
               }
            }
         } else {
            BlockPos pos = this.getBlock();
            if (pos != null) {
               PacketMine.INSTANCE.mine(pos);
            }
         }
      }
   }

   private BlockPos getBlock() {
      BlockPos down = null;

      for (BlockPos pos : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.getEyePosition())) {
         if (!mc.level.isEmptyBlock(pos) && !PacketMine.unbreakable(pos) && BlockUtil.getClickSideStrict(pos) != null) {
            if (!(pos.getY() < mc.player.getY())) {
               return pos;
            }

            if (down == null && this.down.getValue()) {
               down = pos;
            }
         }
      }

      return down;
   }
}
