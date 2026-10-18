package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundSetCarriedItemPacket.class)
public interface IUpdateSelectedSlotC2SPacket {
   @Accessor("slot")
   int getSelectedSlot();
}
