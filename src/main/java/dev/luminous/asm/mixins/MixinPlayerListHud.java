package dev.luminous.asm.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.misc.ExtraTab;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerListHud {
   @Final
   @Shadow
   private static Comparator<PlayerInfo> PLAYER_COMPARATOR;

   @Shadow
   protected abstract List<PlayerInfo> getPlayerInfos();

   @Inject(method = "getPlayerInfos", at = @At("HEAD"), cancellable = true)
   private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerInfo>> cir) {
      if (ExtraTab.INSTANCE.isOn()) {
         cir.setReturnValue(
            Wrapper.mc.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(ExtraTab.INSTANCE.size.getValueInt()).toList()
         );
      }
   }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = Shift.BEFORE))
   private void hookRender(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
      int totalPlayers = this.getPlayerInfos().size();
      int newP = 1;

      int newO;
      for (newO = (totalPlayers + newP - 1) / newP; newO > ExtraTab.INSTANCE.columns.getValueInt(); newO = (totalPlayers + newP - 1) / newP) {
         newP++;
      }

      o.set(newO);
      p.set(newP);
   }
}
