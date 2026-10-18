package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayerEntity implements Wrapper {
   @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("RETURN"), cancellable = true)
   private void poseNotCollide(Pose pose, CallbackInfoReturnable<Boolean> cir) {
      if (Player.class.cast(this) == mc.player && !ClientSetting.INSTANCE.crawl.getValue() && pose == Pose.SWIMMING) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getBlockInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.blockRange.getValue());
      }
   }

   @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
   public void getEntityInteractionRangeHook(CallbackInfoReturnable<Double> cir) {
      if (InteractTweaks.INSTANCE.reach()) {
         cir.setReturnValue(InteractTweaks.INSTANCE.entityRange.getValue());
      }
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravelPre(Vec3 movementInput, CallbackInfo ci) {
      Player player = Player.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Pre, player);
         Alien.EVENT_BUS.post(event);
         if (event.isCancelled()) {
            ci.cancel();
            event = TravelEvent.get(Event.Stage.Post, player);
            Alien.EVENT_BUS.post(event);
         }
      }
   }

   @Inject(method = "travel", at = @At("RETURN"))
   private void onTravelPost(Vec3 movementInput, CallbackInfo ci) {
      Player player = Player.class.cast(this);
      if (player == mc.player) {
         TravelEvent event = TravelEvent.get(Event.Stage.Post, player);
         Alien.EVENT_BUS.post(event);
      }
   }
}
