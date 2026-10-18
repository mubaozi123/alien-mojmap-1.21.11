package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface IEntityVelocityUpdateS2CPacket {
   @Mutable
   @Accessor("velocityX")
   void setX(int var1);

   @Mutable
   @Accessor("velocityY")
   void setY(int var1);

   @Mutable
   @Accessor("velocityZ")
   void setZ(int var1);

   @Accessor("velocityX")
   int getX();

   @Accessor("velocityY")
   int getY();

   @Accessor("velocityZ")
   int getZ();
}
