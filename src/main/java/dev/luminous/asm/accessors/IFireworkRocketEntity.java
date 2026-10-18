package dev.luminous.asm.accessors;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireworkRocketEntity.class)
public interface IFireworkRocketEntity {
   @Accessor("attachedToEntity")
   LivingEntity getShooter();

   @Invoker("isAttachedToEntity")
   boolean hookWasShotByEntity();
}
