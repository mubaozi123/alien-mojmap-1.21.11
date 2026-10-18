package dev.luminous.asm.mixins;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.misc.AutoReconnect;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends Screen {
   @Shadow
   @Final
   private LinearLayout layout;
   @Unique
   private Button reconnectBtn;
   @Unique
   private double time = AutoReconnect.INSTANCE.delay.getValue() * 20.0;

   protected MixinDisconnectedScreen(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = Shift.BEFORE))
   private void addButtons(CallbackInfo ci) {
      if (AutoReconnect.INSTANCE.lastServerConnection != null) {
         this.reconnectBtn = new Builder(Component.literal(this.getText()), button -> this.tryConnecting()).build();
         this.layout.addChild(this.reconnectBtn);
      }
   }

   @Override
   public void tick() {
      if (AutoReconnect.INSTANCE.rejoin() && AutoReconnect.INSTANCE.lastServerConnection != null) {
         if (this.time <= 0.0) {
            this.tryConnecting();
         } else {
            this.time--;
            if (this.reconnectBtn != null) {
               this.reconnectBtn.setMessage(Component.literal(this.getText()));
            }
         }
      }
   }

   @Unique
   private String getText() {
      String reconnectText = "Reconnect";
      if (AutoReconnect.INSTANCE.rejoin()) {
         reconnectText = reconnectText + " " + String.format("(%.1f)", this.time / 20.0);
      }

      return reconnectText;
   }

   @Unique
   private void tryConnecting() {
      Pair<ServerAddress, ServerData> lastServer = AutoReconnect.INSTANCE.lastServerConnection;
      ConnectScreen.startConnecting(new TitleScreen(), Wrapper.mc, (ServerAddress)lastServer.left(), (ServerData)lastServer.right(), false, null);
   }
}
