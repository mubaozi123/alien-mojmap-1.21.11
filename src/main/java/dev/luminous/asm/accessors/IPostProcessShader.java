package dev.luminous.asm.accessors;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface IPostProcessShader {
   @Mutable
   @Accessor("input")
   void setInput(RenderTarget var1);

   @Mutable
   @Accessor("output")
   void setOutput(RenderTarget var1);
}
