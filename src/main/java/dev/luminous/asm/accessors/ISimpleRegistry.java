package dev.luminous.asm.accessors;

import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Holder.Reference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MappedRegistry.class)
public interface ISimpleRegistry<T> {
   @Accessor("byValue")
   Map<T, Reference<T>> getValueToEntry();
}
