package dev.luminous.api.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Render3DScheduler {
   public static final Render3DScheduler INSTANCE = new Render3DScheduler();
   private final List<Render3DScheduler.BoxCommand> boxes = new ArrayList<>();
   private final List<Render3DScheduler.FadeCommand> fades = new ArrayList<>();
   private final List<Render3DScheduler.LineCommand> lines = new ArrayList<>();

   private Render3DScheduler() {
   }

   public void addBox(AABB box, Color fill, Color outline, boolean drawOutline, boolean drawFill) {
      if (drawOutline || drawFill) {
         this.boxes.add(new Render3DScheduler.BoxCommand(box, fill, outline, drawOutline, drawFill));
      }
   }

   public void addFade(AABB box, Color bottom, Color top) {
      this.fades.add(new Render3DScheduler.FadeCommand(box, bottom, top));
   }

   public void addLine(Vec3 start, Vec3 end, Color color, float width) {
      if (start.distanceToSqr(end) >= 1.0E-6) {
         this.lines.add(new Render3DScheduler.LineCommand(start, end, color, width));
      }
   }

   public void flush(PoseStack matrices) {
      try {
         for (Render3DScheduler.BoxCommand command : this.boxes) {
            Render3DUtil.draw3DBox(matrices, command.box, command.fill, command.outline, command.outlineEnabled, command.fillEnabled);
         }

         for (Render3DScheduler.FadeCommand command : this.fades) {
            Render3DUtil.drawFadeFill(matrices, command.box, command.bottom, command.top);
         }

         for (Render3DScheduler.LineCommand command : this.lines) {
            Render3DUtil.drawLine(matrices, command.start, command.end, command.color, command.width);
         }
      } finally {
         this.clear();
      }
   }

   public void clear() {
      this.boxes.clear();
      this.fades.clear();
      this.lines.clear();
   }

   private record BoxCommand(AABB box, Color fill, Color outline, boolean outlineEnabled, boolean fillEnabled) {
   }

   private record FadeCommand(AABB box, Color bottom, Color top) {
   }

   private record LineCommand(Vec3 start, Vec3 end, Color color, float width) {
   }
}
