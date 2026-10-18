package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class HoleESP extends Module {
   public static HoleESP INSTANCE;
   public final SliderSetting startFade = this.add(new SliderSetting("StartFade", 5.0, 1.0, 20.0));
   public final SliderSetting distance = this.add(new SliderSetting("Distance", 6.0, 1.0, 20.0));
   public final SliderSetting airHeight = this.add(new SliderSetting("AirHeight", 1.0, -3.0, 3.0, 0.01));
   public final BooleanSetting airYCheck = this.add(new BooleanSetting("AirYCheck", true));
   public final SliderSetting height = this.add(new SliderSetting("Height", 1.0, -3.0, 3.0, 0.1));
   public final SliderSetting wallHeight = this.add(new SliderSetting("WallHeight", 3.0, -3.0, 3.0, 0.1));
   public final BooleanSetting sideCheck = this.add(new BooleanSetting("SideCheck", true));
   private final ColorSetting airFill = this.add(new ColorSetting("AirFill", new Color(148, 0, 0, 100)).injectBoolean(true));
   private final ColorSetting airBox = this.add(new ColorSetting("AirBox", new Color(148, 0, 0, 100)).injectBoolean(true));
   private final ColorSetting airFade = this.add(new ColorSetting("AirFade", new Color(148, 0, 0, 0)).injectBoolean(true));
   private final ColorSetting normalFill = this.add(new ColorSetting("UnsafeFill", new Color(255, 0, 0, 50)).injectBoolean(true));
   private final ColorSetting normalBox = this.add(new ColorSetting("UnsafeBox", new Color(255, 0, 0, 100)).injectBoolean(true));
   private final ColorSetting normalFade = this.add(new ColorSetting("UnsafeFade", new Color(255, 0, 0, 0)).injectBoolean(true));
   private final ColorSetting bedrockFill = this.add(new ColorSetting("SafeFill", new Color(8, 255, 79, 50)).injectBoolean(true));
   private final ColorSetting bedrockBox = this.add(new ColorSetting("SafeBox", new Color(8, 255, 79, 100)).injectBoolean(true));
   private final ColorSetting bedrockFade = this.add(new ColorSetting("SafeFade", new Color(8, 255, 79, 100)).injectBoolean(true));
   private final ColorSetting wallFill = this.add(new ColorSetting("WallFill", new Color(0, 255, 255, 128)).injectBoolean(true));
   private final ColorSetting wallBox = this.add(new ColorSetting("WallBox", new Color(0, 225, 255, 255)).injectBoolean(true));
   private final ColorSetting wallFade = this.add(new ColorSetting("WallFade", new Color(0, 255, 255, 64)).injectBoolean(true));
   private final ColorSetting wallSideFill = this.add(new ColorSetting("WallSideFill", new Color(0, 255, 255, 128)).injectBoolean(true));
   private final ColorSetting wallSideBox = this.add(new ColorSetting("WallSideBox", new Color(0, 225, 255, 255)).injectBoolean(true));
   private final ColorSetting wallSideFade = this.add(new ColorSetting("WallSideFade", new Color(0, 255, 255, 64)).injectBoolean(true));
   private final SliderSetting updateDelay = this.add(new SliderSetting("UpdateDelay", 50, 0, 1000));
   private final List<BlockPos> tempNormalList = new ArrayList<>();
   private final List<BlockPos> tempBedrockList = new ArrayList<>();
   private final List<BlockPos> tempAirList = new ArrayList<>();
   private final List<BlockPos> tempWallList = new ArrayList<>();
   private final List<BlockPos> tempWallSideList = new ArrayList<>();
   private final Timer timer = new Timer();
   boolean drawing = false;
   private List<BlockPos> normalList = new ArrayList<>();
   private List<BlockPos> bedrockList = new ArrayList<>();
   private List<BlockPos> airList = new ArrayList<>();
   private List<BlockPos> wallList = new ArrayList<>();
   private List<BlockPos> wallSideList = new ArrayList<>();

   public HoleESP() {
      super("HoleESP", Module.Category.Render);
      this.setChinese("坑透视");
      INSTANCE = this;
   }

   public void onThread() {
      if (!nullCheck() && !this.isOff()) {
         if (!this.drawing && this.timer.passedMs(this.updateDelay.getValue())) {
            this.normalList = new ArrayList<>(this.tempNormalList);
            this.bedrockList = new ArrayList<>(this.tempBedrockList);
            this.airList = new ArrayList<>(this.tempAirList);
            this.wallList = new ArrayList<>(this.tempWallList);
            this.wallSideList = new ArrayList<>(this.tempWallSideList);
            this.timer.reset();
            this.tempBedrockList.clear();
            this.tempNormalList.clear();
            this.tempAirList.clear();
            this.tempWallList.clear();
            this.tempWallSideList.clear();

            for (BlockPos pos : BlockUtil.getSphere(this.distance.getValueFloat(), mc.player.position())) {
               if (this.isBedrock(pos) && this.isBedrock(pos.above(2)) && this.isBedrock(pos.below())) {
                  Direction side = this.getWallSide(pos);
                  if (side != null || !this.sideCheck.getValue()) {
                     this.tempWallList.add(pos);
                  }

                  if (side != null) {
                     this.tempWallSideList.add(pos.relative(side));
                  }
               }

               HoleESP.Type type;
               if ((type = this.isHole(pos)) == HoleESP.Type.Bedrock) {
                  this.tempBedrockList.add(pos);
               } else if (type == HoleESP.Type.Normal) {
                  this.tempNormalList.add(pos);
               } else if (type == HoleESP.Type.Air) {
                  this.tempAirList.add(pos);
               }
            }
         }
      }
   }

   private Direction getWallSide(BlockPos pos) {
      double getDistance = Double.MAX_VALUE;
      Direction side = null;

      for (Direction direction : Direction.values()) {
         BlockPos offsetPos;
         if (direction != Direction.UP
            && direction != Direction.DOWN
            && BlockUtil.canCollide(new AABB((offsetPos = pos.relative(direction)).below()))
            && !BlockUtil.canCollide(new AABB(offsetPos))
            && !BlockUtil.canCollide(new AABB(offsetPos.above()))) {
            if (side == null) {
               side = direction;
               getDistance = mc.player.getEyePosition().distanceTo(offsetPos.getCenter());
            } else if (mc.player.getEyePosition().distanceTo(offsetPos.getCenter()) < getDistance) {
               side = direction;
               getDistance = mc.player.getEyePosition().distanceTo(offsetPos.getCenter());
            }
         }
      }

      return side;
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.level.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   HoleESP.Type isHole(BlockPos pos) {
      if (mc.level.isEmptyBlock(pos)
         && (!this.airYCheck.getValue() || pos.getY() == mc.player.getBlockY() - 1 || pos.getY() == mc.player.getBlockY())
         && Alien.HOLE.isHard(pos.above())) {
         return HoleESP.Type.Air;
      } else {
         int blockProgress = 0;
         boolean bedRock = true;

         for (Direction i : Direction.values()) {
            if (i != Direction.UP && i != Direction.DOWN && Alien.HOLE.isHard(pos.relative(i))) {
               if (mc.level.getBlockState(pos.relative(i)).getBlock() != Blocks.BEDROCK) {
                  bedRock = false;
               }

               blockProgress++;
            }
         }

         if (mc.level.isEmptyBlock(pos)
            && mc.level.isEmptyBlock(pos.above())
            && mc.level.isEmptyBlock(pos.above(2))
            && blockProgress > 3
            && BlockUtil.canCollide(mc.player, new AABB(pos.below()))) {
            return bedRock ? HoleESP.Type.Bedrock : HoleESP.Type.Normal;
         } else {
            return Alien.HOLE.isDoubleHole(pos) ? HoleESP.Type.Normal : HoleESP.Type.None;
         }
      }
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      this.drawing = true;
      this.draw(matrixStack, this.bedrockList, this.bedrockFill, this.bedrockFade, this.bedrockBox, this.height.getValue());
      this.draw(matrixStack, this.airList, this.airFill, this.airFade, this.airBox, this.airHeight.getValue());
      this.draw(matrixStack, this.normalList, this.normalFill, this.normalFade, this.normalBox, this.height.getValue());
      this.draw(matrixStack, this.wallList, this.wallFill, this.wallFade, this.wallBox, this.wallHeight.getValue());
      this.draw(matrixStack, this.wallSideList, this.wallSideFill, this.wallSideFade, this.wallSideBox, this.height.getValue());
      this.drawing = false;
   }

   private void draw(PoseStack matrixStack, List<BlockPos> list, ColorSetting fill, ColorSetting fade, ColorSetting box, double height) {
      for (BlockPos pos : list) {
         double getDistance = mc.player.position().distanceTo(pos.getCenter());
         double fadeRange = Math.max(1.0E-4, this.distance.getValue() - this.startFade.getValue());
         double alpha = getDistance > this.startFade.getValue()
            ? Math.max(Math.min(1.0, 1.0 - (getDistance - this.startFade.getValue()) / fadeRange), 0.0)
            : 1.0;
         AABB espBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
         if (fill.booleanValue) {
            if (fade.booleanValue) {
               Render3DScheduler.INSTANCE
                  .addFade(
                     espBox,
                     ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)),
                     ColorUtil.injectAlpha(fade.getValue(), (int)(fade.getValue().getAlpha() * alpha))
                  );
            } else {
               Render3DScheduler.INSTANCE
                  .addBox(espBox, ColorUtil.injectAlpha(fill.getValue(), (int)(fill.getValue().getAlpha() * alpha)), new Color(0, 0, 0, 0), false, true);
            }
         }

         if (box.booleanValue) {
            Render3DScheduler.INSTANCE
               .addBox(espBox, new Color(0, 0, 0, 0), ColorUtil.injectAlpha(box.getValue(), (int)(box.getValue().getAlpha() * alpha)), true, false);
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
