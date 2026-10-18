package dev.luminous.asm.accessors;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Vec3.class)
public interface IVec3d {
   @Mutable
   @Accessor("x")
   void setX(double var1);

   @Mutable
   @Accessor("y")
   void setY(double var1);

   @Mutable
   @Accessor("z")
   void setZ(double var1);
}
