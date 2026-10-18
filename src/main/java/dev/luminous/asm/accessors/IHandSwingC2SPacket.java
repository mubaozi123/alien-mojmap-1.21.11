package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundSwingPacket.class)
public interface IHandSwingC2SPacket {
   @Accessor("hand")
   InteractionHand getHand();
}
