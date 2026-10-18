package dev.luminous.asm.mixins;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor {
   @Final
   @Shadow
   EditBox input;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Final
   @Shadow
   private List<FormattedCharSequence> commandUsage;
   @Unique
   private boolean showOutline = false;

   @Shadow
   public abstract void showSuggestions(boolean var1);

   @Inject(at = @At("HEAD"), method = "render")
   private void onRender(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
      if (this.showOutline) {
         int x = this.input.getX() - 3;
         int y = this.input.getY() - 3;
         Render2DUtil.drawRect(context.pose(), x, y, this.input.getWidth() + 1, 1.0F, ClientSetting.INSTANCE.color.getValue().getRGB());
         Render2DUtil.drawRect(
            context.pose(), x, y + this.input.getHeight() + 1, this.input.getWidth() + 1, 1.0F, ClientSetting.INSTANCE.color.getValue().getRGB()
         );
         Render2DUtil.drawRect(context.pose(), x, y, 1.0F, this.input.getHeight() + 1, ClientSetting.INSTANCE.color.getValue().getRGB());
         Render2DUtil.drawRect(
            context.pose(), x + this.input.getWidth() + 1, y, 1.0F, this.input.getHeight() + 2, ClientSetting.INSTANCE.color.getValue().getRGB()
         );
      }
   }

   @Inject(
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;getCursorPosition()I", ordinal = 0),
      method = "updateCommandInfo()V"
   )
   private void onRefresh(CallbackInfo ci) {
      String prefix = Alien.getPrefix();
      String string = this.input.getValue();
      this.showOutline = string.startsWith(prefix);
      if (!string.isEmpty()) {
         int cursorPos = this.input.getCursorPosition();
         String string2 = string.substring(0, cursorPos);
         if (prefix.startsWith(string2) || string2.startsWith(prefix)) {
            int j = 0;
            Matcher matcher = Pattern.compile("(\\s+)").matcher(string2);

            while (matcher.find()) {
               j = matcher.end();
            }

            SuggestionsBuilder builder = new SuggestionsBuilder(string2, j);
            if (string2.length() < prefix.length()) {
               if (!prefix.startsWith(string2)) {
                  return;
               }

               builder.suggest(prefix);
            } else {
               if (!string2.startsWith(prefix)) {
                  return;
               }

               int count = StringUtils.countMatches(string2, " ");
               List<String> seperated = Arrays.asList(string2.split(" "));
               if (count == 0) {
                  for (Object strObj : Alien.COMMAND.getCommands().keySet().toArray()) {
                     String str = (String)strObj;
                     builder.suggest(Alien.getPrefix() + str + " ");
                  }
               } else {
                  if (seperated.isEmpty()) {
                     return;
                  }

                  Command c = Alien.COMMAND.getCommandBySyntax(seperated.getFirst().substring(prefix.length()));
                  if (c == null) {
                     this.commandUsage
                        .add(Component.nullToEmpty(" §4no commands found: §f" + seperated.getFirst().substring(prefix.length())).getVisualOrderText());
                     return;
                  }

                  String[] suggestions = c.getAutocorrect(count, seperated);
                  if (suggestions == null || suggestions.length == 0) {
                     return;
                  }

                  for (String str : suggestions) {
                     builder.suggest(str + " ");
                  }
               }
            }

            this.pendingSuggestions = builder.buildFuture();
            this.showSuggestions(false);
         }
      }
   }
}
