package dev.luminous.asm.mixins;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.luminous.Alien;
import dev.luminous.api.events.Event;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.DoAttackEvent;
import dev.luminous.api.events.impl.GameLeftEvent;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.events.impl.ResizeEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient extends ReentrantBlockableEventLoop<Runnable> {
   @Shadow
   @Final
   public Gui gui;
   @Shadow
   public int missTime;
   @Shadow
   public LocalPlayer player;
   @Shadow
   public HitResult hitResult;
   @Shadow
   public MultiPlayerGameMode gameMode;
   @Final
   @Shadow
   public ParticleEngine particleEngine;
   @Shadow
   public ClientLevel level;
   @Shadow
   private IntegratedServer singleplayerServer;
   @Shadow
   @Final
   private Window window;

   public MixinMinecraftClient(String string) {
      super(string);
   }

   @Inject(method = "resizeDisplay", at = @At("TAIL"))
   private void captureResize(CallbackInfo ci) {
      Alien.EVENT_BUS.post(new ResizeEvent(this.window));
   }

   @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"), require = 0)
   public void fpsHook(int fps) {
      if (!ClientSetting.INSTANCE.fuckFPSLimit.getValue()) {
         RenderSystem.limitDisplayFPS(fps);
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   void postWindowInit(GameConfig args, CallbackInfo ci) {
      FontManager.init();
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void onSetScreen(Screen screen, CallbackInfo info) {
      OpenScreenEvent event = OpenScreenEvent.get(screen);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = "startAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;", shift = Shift.BEFORE)
   )
   public void onAttack(CallbackInfoReturnable<Boolean> cir) {
      Alien.EVENT_BUS.post(DoAttackEvent.getPre());
   }

   @Inject(
      method = "startAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V", shift = Shift.AFTER)
   )
   public void onAttackPost(CallbackInfoReturnable<Boolean> cir) {
      Alien.EVENT_BUS.post(DoAttackEvent.getPost());
   }

   @Inject(method = "disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
   private void onGameLeave(Component reasonText, CallbackInfo ci) {
      if (this.level != null) {
         Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }

      if (ClientSetting.INSTANCE.titleFix.getValue()) {
         this.gui.clearTitles();
         this.gui.resetTitleTimes();
      }
   }

   @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
   private void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
      if (this.missTime <= 0 && this.player.isUsingItem() && InteractTweaks.INSTANCE.multiTask()) {
         if (breaking && this.hitResult != null && this.hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)this.hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (!this.level.getBlockState(blockPos).isAir()) {
               Direction direction = blockHitResult.getDirection();
               if (this.gameMode.continueDestroyBlock(blockPos, direction)) {
                  this.level.addBreakingBlockEffect(blockPos, direction);
                  this.player.swing(InteractionHand.MAIN_HAND);
               }
            }
         } else {
            this.gameMode.stopDestroyBlock();
         }

         ci.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "tick()V")
   public void tickHead(CallbackInfo info) {
      try {
         Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Pre));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [HEAD]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Inject(at = @At("TAIL"), method = "tick()V")
   public void tickTail(CallbackInfo info) {
      try {
         Alien.EVENT_BUS.post(ClientTickEvent.get(Event.Stage.Post));
      } catch (Exception var3) {
         var3.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (MinecraftClient.tick() [TAIL]) Message: [" + var3.getMessage() + "]");
         }
      }
   }

   @Overwrite
   private String createTitle() {
      if (ClientSetting.INSTANCE == null) {
         return "Alien: Loading..";
      } else if (ClientSetting.INSTANCE.titleOverride.getValue()) {
         return ClientSetting.INSTANCE.windowTitle.getValue();
      } else {
         StringBuilder stringBuilder = new StringBuilder(ClientSetting.INSTANCE.windowTitle.getValue());
         stringBuilder.append(" ");
         stringBuilder.append(SharedConstants.getCurrentVersion().name());
         ClientPacketListener clientPlayNetworkHandler = this.getConnection();
         if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isConnected()) {
            stringBuilder.append(" - ");
            ServerData serverInfo = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
               stringBuilder.append(I18n.get("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
               stringBuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer != null || serverInfo != null && serverInfo.isLan()) {
               stringBuilder.append(I18n.get("title.multiplayer.lan"));
            } else {
               stringBuilder.append(I18n.get("title.multiplayer.other"));
            }
         }

         return stringBuilder.toString();
      }
   }

   @Shadow
   public ClientPacketListener getConnection() {
      return null;
   }

   @Shadow
   public ServerData getCurrentServer() {
      return null;
   }
}
