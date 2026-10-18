package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.common.ServerboundPongPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundPongPacket.class)
public interface ICommonPongC2SPacket {
   @Accessor("id")
   int getParameter();
}
