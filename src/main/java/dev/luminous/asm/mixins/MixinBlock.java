package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.movement.NoSlow;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock implements ItemLike {
   @Inject(at = @At("HEAD"), method = "getSpeedFactor()F", cancellable = true)
   private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
      if (NoSlow.INSTANCE.soulSand() && cir.getReturnValueF() < 1.0F) {
         cir.setReturnValue(1.0F);
      }
   }
}
