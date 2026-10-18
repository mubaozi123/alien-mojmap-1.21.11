package dev.luminous.asm.accessors;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface IClientPlayerEntity {
   @Accessor("positionReminder")
   void setTicksSinceLastPositionPacketSent(int var1);

   @Accessor("yRotLast")
   float getLastYaw();

   @Accessor("yRotLast")
   void setLastYaw(float var1);

   @Accessor("xRotLast")
   float getLastPitch();

   @Accessor("xRotLast")
   void setLastPitch(float var1);
}
