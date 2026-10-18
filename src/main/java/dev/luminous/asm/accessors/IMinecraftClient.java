package dev.luminous.asm.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraftClient {
   @Mutable
   @Accessor("user")
   void setSession(User var1);

   @Invoker("startUseItem")
   void invokeDoItemUse();
}
