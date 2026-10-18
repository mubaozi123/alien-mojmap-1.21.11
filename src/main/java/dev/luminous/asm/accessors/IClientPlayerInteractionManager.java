package dev.luminous.asm.accessors;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface IClientPlayerInteractionManager {
   @Accessor("carriedIndex")
   int getLastSelectedSlot();

   @Accessor("carriedIndex")
   void setLastSelectedSlot(int var1);
}
