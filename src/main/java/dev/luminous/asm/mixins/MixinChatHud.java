package dev.luminous.asm.mixins;

import dev.luminous.api.interfaces.IChatHudHook;
import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.render.ChatHudUtil;
import dev.luminous.asm.accessors.IChatHud;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHudHook {
   @Unique
   private int nextMessageId = 0;
   @Unique
   private boolean nextSync;
   @Final
   @Shadow
   private List<Line> trimmedMessages;
   @Shadow
   @Final
   private List<GuiMessage> allMessages;

   @Inject(method = "<init>", at = @At("TAIL"))
   public void onInit(Minecraft client, CallbackInfo ci) {
      ((IChatHud)this).setMessages(new CopyOnWriteArrayList<>());
      ((IChatHud)this).setVisibleMessages(new CopyOnWriteArrayList<>());
   }

   @Override
   public void alienClient$addMessage(Component message, int id) {
      this.nextMessageId = id;
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
      this.nextMessageId = 0;
   }

   @Override
   public void alienClient$addMessage(Component message) {
      this.nextSync = true;
      this.addMessage(message);
      this.nextSync = false;
   }

   @Override
   public void alienClient$addMessageOutSync(Component message, int id) {
      this.nextMessageId = id;
      this.addMessage(message);
      this.nextMessageId = 0;
   }

   @Inject(
      method = "addMessageToDisplayQueue",
      at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER, remap = false)
   )
   private void onAddMessageAfterNewChatHudLineVisible(GuiMessage message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.trimmedMessages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
      at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V", ordinal = 0, shift = Shift.AFTER, remap = false)
   )
   private void onAddMessageAfterNewChatHudLine(GuiMessage message, CallbackInfo ci) {
      IChatHudLineHook line = (IChatHudLineHook)(Object)this.allMessages.getFirst();
      if (line != null) {
         line.alienClient$setMessageId(this.nextMessageId);
         line.alienClient$setSync(this.nextSync);
         line.alienClient$setFade(new FadeUtils(ClientSetting.INSTANCE.animationTime.getValueInt()));
      }
   }

   @Inject(
      at = @At("HEAD"),
      method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"
   )
   private void onAddMessage(Component message, MessageSignature signatureData, GuiMessageTag indicator, CallbackInfo ci) {
      if (this.nextMessageId != 0) {
         this.trimmedMessages.removeIf(msg -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
         this.allMessages.removeIf(msg -> ((IChatHudLineHook)(Object)msg).alienClient$getMessageId() == this.nextMessageId);
      }
   }

   @Redirect(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2, remap = false), require = 0)
   public int chatLinesSize(List<Line> list) {
      return ClientSetting.INSTANCE.isOn() && ClientSetting.INSTANCE.infiniteChat.getValue() ? Integer.MIN_VALUE : list.size();
   }

   @ModifyVariable(
      method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V",
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private ChatGraphicsAccess chatHudAnimate(ChatGraphicsAccess drawer) {
      return (ChatGraphicsAccess)(!ClientSetting.INSTANCE.isOn() ? drawer : new ChatHudUtil(drawer, this::alienClient$findLine));
   }

   @Unique
   private IChatHudLineHook alienClient$findLine(FormattedCharSequence text) {
      for (Line visible : this.trimmedMessages) {
         if (visible.content() == text) {
            return (IChatHudLineHook)(Object)visible;
         }
      }

      return null;
   }

   @Shadow
   public abstract void addMessage(Component var1);

   @Shadow
   public int getWidth() {
      return 0;
   }
}
