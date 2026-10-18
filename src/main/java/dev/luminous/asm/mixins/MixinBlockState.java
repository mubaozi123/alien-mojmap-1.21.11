package dev.luminous.asm.mixins;

import com.mojang.serialization.MapCodec;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.BlockActivateEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class MixinBlockState extends BlockStateBase {
   public MixinBlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
      super(block, propertyMap, mapCodec);
   }

   @Override
   public InteractionResult useWithoutItem(Level world, Player player, BlockHitResult hit) {
      Alien.EVENT_BUS.post(BlockActivateEvent.get(BlockState.class.cast(this)));
      return super.useWithoutItem(world, player, hit);
   }
}
