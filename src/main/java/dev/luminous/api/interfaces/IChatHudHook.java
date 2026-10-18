package dev.luminous.api.interfaces;

import net.minecraft.network.chat.Component;

public interface IChatHudHook {
   void alienClient$addMessage(Component var1, int var2);

   void alienClient$addMessage(Component var1);

   void alienClient$addMessageOutSync(Component var1, int var2);
}
