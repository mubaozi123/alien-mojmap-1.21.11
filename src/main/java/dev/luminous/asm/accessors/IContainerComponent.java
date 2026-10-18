package dev.luminous.asm.accessors;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemContainerContents.class)
public interface IContainerComponent {
   @Accessor("items")
   NonNullList<ItemStack> getStacks();
}
