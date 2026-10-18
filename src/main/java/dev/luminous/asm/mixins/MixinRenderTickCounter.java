package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.TimerEvent;
import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Timer.class)
public class MixinRenderTickCounter {
   @Shadow
   private float deltaTicks;
   @Shadow
   private float deltaTickResidual;
   @Shadow
   private long lastMs;
   @Final
   @Shadow
   private float msPerTick;

   @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"), cancellable = true)
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TimerEvent event = TimerEvent.getEvent();
      Alien.EVENT_BUS.post(event);
      if (!event.isCancelled()) {
         float timer = event.isModified() ? event.get() : Alien.TIMER.get();
         if (timer == 1.0F) {
            return;
         }

         this.deltaTicks = (float)(timeMillis - this.lastMs) / this.msPerTick * timer;
         this.lastMs = timeMillis;
         this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
         int i = (int)this.deltaTickResidual;
         this.deltaTickResidual -= i;
         cir.setReturnValue(i);
      }
   }
}
