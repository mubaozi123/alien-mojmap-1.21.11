package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.ServerConnectBeginEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen {
   @Inject(
      method = "startConnecting(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;ZLnet/minecraft/client/multiplayer/TransferState;)V",
      at = @At("HEAD")
   )
   private static void tryConnectEvent(
      Screen screen, Minecraft client, ServerAddress address, ServerData info, boolean quickPlay, TransferState cookieStorage, CallbackInfo ci
   ) {
      Alien.EVENT_BUS.post(ServerConnectBeginEvent.get(address, info));
   }

   @Inject(
      method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V",
      at = @At("HEAD")
   )
   private void tryConnectEvent(Minecraft client, ServerAddress address, ServerData info, TransferState cookieStorage, CallbackInfo ci) {
      Alien.EVENT_BUS.post(ServerConnectBeginEvent.get(address, info));
   }
}
