package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemOnPacket.class)
public interface IPlayerInteractBlockC2SPacket {
   @Accessor("blockHit")
   BlockHitResult getBlockHitResult();

   @Accessor("hand")
   InteractionHand getHand();
}
