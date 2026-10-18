package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.ChunkOcclusionEvent;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VisGraph.class)
public abstract class MixinChunkOcclusionDataBuilder {
   @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
   private void onMarkClosed(BlockPos pos, CallbackInfo info) {
      ChunkOcclusionEvent event = Alien.EVENT_BUS.post(ChunkOcclusionEvent.get());
      if (event.isCancelled()) {
         info.cancel();
      }
   }
}
