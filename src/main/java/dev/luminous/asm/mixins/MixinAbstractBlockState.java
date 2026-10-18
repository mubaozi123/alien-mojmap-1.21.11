package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.render.Ambience;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public class MixinAbstractBlockState {
   @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
   public void getLuminanceHook(CallbackInfoReturnable<Integer> cir) {
      if (!Module.nullCheck()) {
         if (Ambience.INSTANCE.customLuminance.getValue()) {
            cir.setReturnValue(Ambience.INSTANCE.luminance.getValueInt());
         }
      }
   }
}
