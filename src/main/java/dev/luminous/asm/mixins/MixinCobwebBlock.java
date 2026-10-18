package dev.luminous.asm.mixins;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.movement.FastWeb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WebBlock.class)
public class MixinCobwebBlock {
   @Inject(at = @At("HEAD"), method = "entityInside", cancellable = true)
   private void onGetVelocityMultiplier(
      BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl, CallbackInfo ci
   ) {
      if (FastWeb.INSTANCE.isOn() && (Wrapper.mc.options.keyShift.isDown() || !FastWeb.INSTANCE.onlySneak.getValue())) {
         if (FastWeb.INSTANCE.mode.is(FastWeb.Mode.Ignore)) {
            ci.cancel();
            entity.resetFallDistance();
         } else if (FastWeb.INSTANCE.mode.is(FastWeb.Mode.Custom)) {
            ci.cancel();
            entity.makeStuckInBlock(
               state,
               new Vec3(FastWeb.INSTANCE.xZSlow.getValue() / 100.0, FastWeb.INSTANCE.ySlow.getValue() / 100.0, FastWeb.INSTANCE.xZSlow.getValue() / 100.0)
            );
         }
      }
   }
}
