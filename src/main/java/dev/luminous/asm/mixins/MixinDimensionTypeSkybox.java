package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Ambience;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public class MixinDimensionTypeSkybox {
   @Inject(method = "skybox", at = @At("HEAD"), cancellable = true)
   private void hookSkybox(CallbackInfoReturnable<Skybox> cir) {
      if (Ambience.INSTANCE.isOn() && Ambience.INSTANCE.forceOverworld.getValue()) {
         cir.setReturnValue(Skybox.OVERWORLD);
      }
   }
}
