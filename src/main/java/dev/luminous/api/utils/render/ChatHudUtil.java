package dev.luminous.api.utils.render;

import dev.luminous.api.interfaces.IChatHudLineHook;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.GuiMessageTag.Icon;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

public final class ChatHudUtil implements ChatGraphicsAccess {
   private final ChatGraphicsAccess delegate;
   private final Function<FormattedCharSequence, IChatHudLineHook> finder;

   public ChatHudUtil(ChatGraphicsAccess delegate, Function<FormattedCharSequence, IChatHudLineHook> finder) {
      this.delegate = delegate;
      this.finder = finder;
   }

   @Override
   public void updatePose(Consumer<Matrix3x2f> transformer) {
      this.delegate.updatePose(transformer);
   }

   @Override
   public void fill(int x1, int y1, int x2, int y2, int color) {
      this.delegate.fill(x1, y1, x2, y2, color);
   }

   @Override
   public boolean handleMessage(int y, float opacity, FormattedCharSequence text) {
      IChatHudLineHook line = this.finder.apply(text);
      if (line == null) {
         return this.delegate.handleMessage(y, opacity, text);
      } else {
         FadeUtils fadeUtils = line.alienClient$getFade();
         double ease = fadeUtils == null ? 1.0 : fadeUtils.ease(ClientSetting.INSTANCE.ease.getValue());
         double fade = 1.0 - ease;
         if (line.alienClient$getSync() && ClientSetting.INSTANCE.isOn()) {
            text = recolor(text, ClientSetting.INSTANCE.color.getValue().getRGB());
         }

         int offset = (int)(fade * ClientSetting.INSTANCE.animateOffset.getValue());
         if (offset != 0) {
            this.delegate.updatePose(m -> m.translate(offset, 0.0F));
         }

         float newOpacity = opacity;
         if (ClientSetting.INSTANCE.fade.getValue()) {
            newOpacity = (float)(opacity * Math.max(0.04, ease));
         }

         boolean result = this.delegate.handleMessage(y, newOpacity, text);
         if (offset != 0) {
            this.delegate.updatePose(m -> m.translate(-offset, 0.0F));
         }

         return result;
      }
   }

   private static FormattedCharSequence recolor(FormattedCharSequence text, int color) {
      return visitor -> text.accept(
         (index, style, codePoint) -> style.getColor() != null
            ? visitor.accept(index, style, codePoint)
            : visitor.accept(index, style.withColor(color), codePoint)
      );
   }

   @Override
   public void handleTag(int x1, int y1, int x2, int y2, float opacity, GuiMessageTag indicator) {
      if (!ClientSetting.INSTANCE.hideIndicator.getValue()) {
         this.delegate.handleTag(x1, y1, x2, y2, opacity, indicator);
      }
   }

   @Override
   public void handleTagIcon(int left, int bottom, boolean forceDraw, GuiMessageTag indicator, Icon icon) {
      if (!ClientSetting.INSTANCE.hideIndicator.getValue()) {
         this.delegate.handleTagIcon(left, bottom, forceDraw, indicator, icon);
      }
   }
}
