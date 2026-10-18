package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientboundPlayerPositionPacket.class)
public interface IPlayerPositionLookS2CPacket {
   @Invoker("change")
   PositionMoveRotation invokeChange();
}
