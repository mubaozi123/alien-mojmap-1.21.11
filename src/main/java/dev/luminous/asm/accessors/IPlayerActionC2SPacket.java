package dev.luminous.asm.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundPlayerActionPacket.class)
public interface IPlayerActionC2SPacket {
   @Accessor("pos")
   BlockPos getPos();

   @Accessor("direction")
   Direction getDirection();

   @Accessor("action")
   Action getAction();
}
