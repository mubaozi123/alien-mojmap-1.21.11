package dev.luminous.asm.accessors;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Explosion.class)
public interface IExplosion {
   @Mutable
   @Accessor("x")
   void setX(double var1);

   @Mutable
   @Accessor("y")
   void setY(double var1);

   @Mutable
   @Accessor("z")
   void setZ(double var1);

   @Mutable
   @Accessor("power")
   void setPower(float var1);

   @Mutable
   @Accessor("entity")
   void setEntity(Entity var1);

   @Mutable
   @Accessor("world")
   Level getWorld();

   @Mutable
   @Accessor("world")
   void setWorld(Level var1);

   @Mutable
   @Accessor("damageSource")
   DamageSource getDamageSource();
}
