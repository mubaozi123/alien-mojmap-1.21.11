package dev.luminous.asm.accessors;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface IScreenHandler {
   @Accessor("stateId")
   int getRevision();

   @Accessor("stateId")
   void setRevision(int var1);
}
