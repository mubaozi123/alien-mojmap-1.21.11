package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

public class Tracers extends Module {
   private final ColorSetting item = this.add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting player = this.add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting chest = this.add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
   private final ColorSetting enderChest = this.add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
   private final ColorSetting shulkerBox = this.add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));

   public Tracers() {
      super("Tracers", Module.Category.Render);
      this.setChinese("追踪者");
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      mc.options.bobView().set(false);
      if (this.item.booleanValue || this.player.booleanValue) {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof ItemEntity && this.item.booleanValue) {
               this.drawLine(entity.position(), this.item.getValue());
            } else if (entity instanceof Player && this.player.booleanValue && entity != mc.player) {
               this.drawLine(entity.position(), this.player.getValue());
            }
         }
      }

      for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
         if (blockEntity instanceof ChestBlockEntity && this.chest.booleanValue) {
            this.drawLine(blockEntity.getBlockPos().getCenter(), this.chest.getValue());
         } else if (blockEntity instanceof EnderChestBlockEntity && this.enderChest.booleanValue) {
            this.drawLine(blockEntity.getBlockPos().getCenter(), this.enderChest.getValue());
         } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBox.booleanValue) {
            this.drawLine(blockEntity.getBlockPos().getCenter(), this.shulkerBox.getValue());
         }
      }
   }

   private void drawLine(Vec3 pos, Color color) {
      Render3DUtil.drawLine(
         pos,
         mc.gameRenderer
            .getMainCamera()
            .position()
            .add(
               Vec3.directionFromRotation(
                     mc.player.getViewXRot(mc.getDeltaTracker().getGameTimeDeltaTicks()), mc.player.getViewYRot(mc.getDeltaTracker().getGameTimeDeltaTicks())
                  )
                  .scale(0.2)
            ),
         color
      );
   }
}
