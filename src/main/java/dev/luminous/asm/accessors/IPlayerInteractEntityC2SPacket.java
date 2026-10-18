package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface IPlayerInteractEntityC2SPacket {
   @Accessor("entityId")
   int getEntityId();
}
