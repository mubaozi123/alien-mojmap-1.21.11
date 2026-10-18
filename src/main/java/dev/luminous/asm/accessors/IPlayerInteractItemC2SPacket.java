package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemPacket.class)
public interface IPlayerInteractItemC2SPacket {
   @Accessor("hand")
   InteractionHand getHand();
}
