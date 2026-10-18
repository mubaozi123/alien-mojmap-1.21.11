package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class PhaseESP extends Module {
   public static PhaseESP INSTANCE;
   private final SliderSetting distance = this.add(new SliderSetting("Distance", 0.1, 0.0, 1.0, 0.1));
   private final SliderSetting bevelDistance = this.add(new SliderSetting("BevelDistance", 0.2, 0.0, 1.0, 0.1));
   private final ColorSetting safeFill = this.add(new ColorSetting("SafeFill", new Color(0, 255, 0, 50)).injectBoolean(true));
   private final ColorSetting safeBox = this.add(new ColorSetting("SafeBox", new Color(0, 255, 0, 100)).injectBoolean(true));
   private final ColorSetting semiSafeFill = this.add(new ColorSetting("SemiSafeFill", new Color(244, 255, 0, 50)).injectBoolean(true));
   private final ColorSetting semiSafeBox = this.add(new ColorSetting("SemiSafeBox", new Color(244, 255, 0, 100)).injectBoolean(true));
   private final ColorSetting unsafeFill = this.add(new ColorSetting("UnsafeFill", new Color(148, 0, 0, 50)).injectBoolean(true));
   private final ColorSetting unsafeBox = this.add(new ColorSetting("UnsafeBox", new Color(148, 0, 0, 100)).injectBoolean(true));
   List<BlockPos> safe = new ArrayList<>();
   List<BlockPos> semiSafe = new ArrayList<>();
   List<BlockPos> unsafe = new ArrayList<>();
   int[] offsets = new int[]{1, 0, -1};

   public PhaseESP() {
      super("PhaseESP", Module.Category.Render);
      this.setChinese("穿墙显示");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.safe.clear();
      this.semiSafe.clear();
      this.unsafe.clear();

      for (int x : this.offsets) {
         for (int z : this.offsets) {
            BlockPos pos = mc.player.blockPosition().offset(x, 0, z);
            if (mc.player.position().distanceTo(pos.getBottomCenter())
               <= (x != 0 && z != 0 ? this.bevelDistance.getValue() + 1.0 : this.distance.getValue() + 0.8)) {
               BlockState blockState = mc.level.getBlockState(pos);
               BlockPos downPos = pos.below();
               if (blockState.getBlock() == Blocks.BEDROCK) {
                  Block downBlock = mc.level.getBlockState(downPos).getBlock();
                  if (downBlock == Blocks.BEDROCK) {
                     this.safe.add(pos);
                  } else {
                     this.unsafe.add(pos);
                  }
               } else if (blockState.isCollisionShapeFullBlock(mc.level, pos)) {
                  Block downBlock = mc.level.getBlockState(downPos).getBlock();
                  if (downBlock == Blocks.BEDROCK) {
                     this.semiSafe.add(pos);
                  } else {
                     this.unsafe.add(pos);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      this.draw(matrixStack, this.safe, this.safeFill, this.safeBox);
      this.draw(matrixStack, this.unsafe, this.unsafeFill, this.unsafeBox);
      this.draw(matrixStack, this.semiSafe, this.semiSafeFill, this.semiSafeBox);
   }

   private void draw(PoseStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting box) {
      for (BlockPos pos : list) {
         AABB espBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY(), pos.getZ() + 1);
         if (fill.booleanValue) {
            Render3DUtil.drawFill(matrixStack, espBox, fill.getValue());
         }

         if (box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, espBox, box.getValue());
         }
      }
   }

   public static enum Type {
      None,
      Air,
      Normal,
      Bedrock;
   }
}
