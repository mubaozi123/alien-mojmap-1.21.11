package dev.luminous.asm.accessors;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface IHeldItemRenderer {
   @Accessor("mainHandHeight")
   float getEquippedProgressMainHand();

   @Accessor("mainHandHeight")
   void setEquippedProgressMainHand(float var1);

   @Accessor("offHandHeight")
   float getEquippedProgressOffHand();

   @Accessor("offHandHeight")
   void setEquippedProgressOffHand(float var1);

   @Accessor("mainHandItem")
   void setItemStackMainHand(ItemStack var1);

   @Accessor("offHandItem")
   void setItemStackOffHand(ItemStack var1);
}
