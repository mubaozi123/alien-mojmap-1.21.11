package dev.luminous.asm.accessors;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundExplodePacket.class)
public interface IExplosionS2CPacket {
   @Mutable
   @Accessor("playerVelocityX")
   void setVelocityX(float var1);

   @Mutable
   @Accessor("playerVelocityY")
   void setVelocityY(float var1);

   @Mutable
   @Accessor("playerVelocityZ")
   void setVelocityZ(float var1);

   @Accessor("playerVelocityX")
   float getX();

   @Accessor("playerVelocityY")
   float getY();

   @Accessor("playerVelocityZ")
   float getZ();
}
