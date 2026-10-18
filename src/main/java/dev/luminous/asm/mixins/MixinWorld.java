package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinWorld {
   @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
   public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
      if (Wrapper.mc.level != null && Wrapper.mc.level.isInWorldBounds(pos)) {
         if (SelfTrap.airList.contains(pos)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
         }

         if (!ClientSetting.INSTANCE.mioCompatible.getValue()) {
            boolean terrainIgnore = CombatUtil.terrainIgnore;
            BlockPos modifyPos = CombatUtil.modifyPos;
            BlockState modifyBlockState = CombatUtil.modifyBlockState;
            if (terrainIgnore || modifyPos != null) {
               LevelChunk worldChunk = Wrapper.mc.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
               BlockState tempState = worldChunk.getBlockState(pos);
               if (modifyPos != null && modifyBlockState != null && pos.equals(modifyPos)) {
                  cir.setReturnValue(modifyBlockState);
                  return;
               }

               if (terrainIgnore) {
                  if (Alien.HOLE.isHard(tempState.getBlock())) {
                     return;
                  }

                  cir.setReturnValue(Blocks.AIR.defaultBlockState());
               }
            } else if (InteractTweaks.INSTANCE.isActive) {
               LevelChunk worldChunkx = Wrapper.mc.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
               BlockState tempStatex = worldChunkx.getBlockState(pos);
               if (tempStatex.getBlock() == Blocks.BEDROCK) {
                  cir.setReturnValue(Blocks.AIR.defaultBlockState());
               }
            }
         }
      }
   }
}
