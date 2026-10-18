package dev.luminous.asm.accessors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IEntity {
   @Mutable
   @Accessor("position")
   void setPos(Vec3 var1);

   @Accessor("dimensions")
   EntityDimensions getDimensions();
}
