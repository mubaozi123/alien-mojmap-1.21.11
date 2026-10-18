package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.exploit.AntiPacket;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinPacketByteBuf {
   @ModifyArg(
      method = "readNbt(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/nbt/CompoundTag;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/FriendlyByteBuf;readNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/Tag;"
      )
   )
   private static NbtAccounter xlPackets(NbtAccounter sizeTracker) {
      return AntiPacket.INSTANCE.isOn() && AntiPacket.INSTANCE.decode.getValue() ? NbtAccounter.unlimitedHeap() : sizeTracker;
   }
}
