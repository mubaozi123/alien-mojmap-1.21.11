package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundContainerClosePacket.class)
public interface ICloseHandledScreenC2SPacket {
   @Accessor("containerId")
   int getSyncId();
}
