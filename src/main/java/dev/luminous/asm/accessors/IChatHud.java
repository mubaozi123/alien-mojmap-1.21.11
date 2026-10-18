package dev.luminous.asm.accessors;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessage.Line;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface IChatHud {
   @Mutable
   @Accessor("trimmedMessages")
   void setVisibleMessages(List<Line> var1);

   @Mutable
   @Accessor("allMessages")
   void setMessages(List<GuiMessage> var1);
}
