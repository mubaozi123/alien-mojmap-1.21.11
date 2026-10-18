package dev.luminous.asm.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luminous.mod.modules.impl.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class SodiumBlockRenderContextMixin {
   @Shadow
   protected BlockState state;
   @Shadow
   protected BlockPos pos;
   @Shadow
   protected BlockAndTintGetter level;

   @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
   private boolean onShouldDrawSide(boolean original, Direction facing) {
      Xray xray = Xray.INSTANCE;
      return xray.isOn() && this.state != null && this.pos != null && this.level != null
         ? xray.modifyDrawSide(this.state, this.level, this.pos, facing, original)
         : original;
   }
}
