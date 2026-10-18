package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.movement.Velocity;
import java.util.Iterator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlowingFluid.class)
public class MixinFlowableFluid {
   @Redirect(method = "getFlow", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0), require = 0)
   private boolean getVelocity_hasNext(Iterator<Direction> var9) {
      return Velocity.INSTANCE.isOn() && Velocity.INSTANCE.waterPush.getValue() ? false : var9.hasNext();
   }
}
