package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.AmbientOcclusionEvent;
import dev.luminous.api.events.impl.ChunkOcclusionEvent;
import dev.luminous.api.events.impl.RenderBlockEntityEvent;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

public class Xray extends Module {
   public static Xray INSTANCE;
   public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false).injectTask(this::openGui));
   private static final ThreadLocal<MutableBlockPos> EXPOSED_POS = ThreadLocal.withInitial(MutableBlockPos::new);

   public Xray() {
      super("Xray", Module.Category.Render);
      this.setChinese("矿物透视");
      INSTANCE = this;
   }

   private void openGui() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.XRAY)));
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDisable() {
      mc.levelRenderer.allChanged();
   }

   public boolean isBlocked(Block block) {
      return !Alien.XRAY.inWhitelist(block.getDescriptionId());
   }

   @EventListener
   private void onRenderBlockEntity(RenderBlockEntityEvent event) {
      if (this.isBlocked(event.blockState.getBlock())) {
         event.cancel();
      }
   }

   @EventListener
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   @EventListener
   private void onAmbientOcclusion(AmbientOcclusionEvent event) {
      event.lightLevel = 1.0F;
   }

   public static boolean shouldBlock(BlockState state) {
      return INSTANCE.isOn() && INSTANCE.isBlocked(state.getBlock());
   }

   public boolean modifyDrawSide(BlockState state, BlockGetter view, BlockPos pos, Direction facing, boolean returns) {
      return !returns && !this.isBlocked(state.getBlock()) ? this.shouldReveal(state, view, pos.relative(facing), facing) : returns;
   }

   public boolean modifyDrawSideAtNeighbor(BlockState state, BlockGetter view, BlockPos neighborPos, Direction facing, boolean returns) {
      return !returns && !this.isBlocked(state.getBlock()) ? this.shouldReveal(state, view, neighborPos, facing) : returns;
   }

   private boolean shouldReveal(BlockState state, BlockGetter view, BlockPos neighborPos, Direction facing) {
      BlockState neighborState = view.getBlockState(neighborPos);
      return neighborState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block()
         || neighborState.getBlock() != state.getBlock()
         || isExposed(view, neighborPos);
   }

   public static boolean isExposed(BlockPos blockPos) {
      return isExposed(mc.level, blockPos);
   }

   public static boolean isExposed(BlockGetter view, BlockPos blockPos) {
      if (view == null) {
         return true;
      } else {
         for (Direction direction : Direction.values()) {
            if (!view.getBlockState(EXPOSED_POS.get().setWithOffset(blockPos, direction)).canOcclude()) {
               return true;
            }
         }

         return false;
      }
   }
}
