package dev.luminous.asm.accessors;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRenderer {
   @Invoker("renderItemInHand")
   void IRenderHand(float var1, boolean var2, Matrix4f var3);

   @Accessor("fogRenderer")
   FogRenderer getFogRenderer();
}
