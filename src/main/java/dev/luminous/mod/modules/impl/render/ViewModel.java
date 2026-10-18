package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.HeldItemRendererEvent;
import dev.luminous.asm.accessors.IHeldItemRenderer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.world.InteractionHand;

public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   public final BooleanSetting mainhandSwap = this.add(new BooleanSetting("MainhandSwap", true));
   public final BooleanSetting offhandSwap = this.add(new BooleanSetting("OffhandSwap", true));
   public final SliderSetting scaleMainX = this.add(new SliderSetting("ScaleMainX", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting scaleMainY = this.add(new SliderSetting("ScaleMainY", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting scaleMainZ = this.add(new SliderSetting("ScaleMainZ", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting positionMainX = this.add(new SliderSetting("PositionMainX", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting positionMainY = this.add(new SliderSetting("PositionMainY", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting positionMainZ = this.add(new SliderSetting("PositionMainZ", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting rotationMainX = this.add(new SliderSetting("RotationMainX", 0.0, -180.0, 180.0, 0.01));
   public final SliderSetting rotationMainY = this.add(new SliderSetting("RotationMainY", 0.0, -180.0, 180.0, 0.01));
   public final SliderSetting rotationMainZ = this.add(new SliderSetting("RotationMainZ", 0.0, -180.0, 180.0, 0.01));
   public final SliderSetting scaleOffX = this.add(new SliderSetting("ScaleOffX", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting scaleOffY = this.add(new SliderSetting("ScaleOffY", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting scaleOffZ = this.add(new SliderSetting("ScaleOffZ", 1.0, 0.1F, 5.0, 0.01));
   public final SliderSetting positionOffX = this.add(new SliderSetting("PositionOffX", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting positionOffY = this.add(new SliderSetting("PositionOffY", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting positionOffZ = this.add(new SliderSetting("PositionOffZ", 0.0, -3.0, 3.0, 0.01));
   public final SliderSetting rotationOffX = this.add(new SliderSetting("RotationOffX", 0.0, -180.0, 180.0, 0.01));
   public final SliderSetting rotationOffY = this.add(new SliderSetting("RotationOffY", 0.0, -180.0, 180.0, 0.01));
   public final SliderSetting rotationOffZ = this.add(new SliderSetting("RotationOffZ", 0.0, -180.0, 180.0, 0.01));
   public final BooleanSetting slowAnimation = this.add(new BooleanSetting("SwingSpeed", true));
   public final SliderSetting slowAnimationVal = this.add(new SliderSetting("Value", 6, 1, 50));

   public ViewModel() {
      super("ViewModel", Module.Category.Render);
      this.setChinese("手持模型");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (!this.mainhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressMainHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressMainHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackMainHand(mc.player.getMainHandItem());
      }

      if (!this.offhandSwap.getValue() && ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).getEquippedProgressOffHand() <= 1.0F) {
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setEquippedProgressOffHand(1.0F);
         ((IHeldItemRenderer)mc.getEntityRenderDispatcher().getItemInHandRenderer()).setItemStackOffHand(mc.player.getOffhandItem());
      }
   }

   @EventListener
   private void onHeldItemRender(HeldItemRendererEvent event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         event.getStack().translate(this.positionMainX.getValueFloat(), this.positionMainY.getValueFloat(), this.positionMainZ.getValueFloat());
         event.getStack().scale(this.scaleMainX.getValueFloat(), this.scaleMainY.getValueFloat(), this.scaleMainZ.getValueFloat());
         event.getStack().mulPose(Axis.XP.rotationDegrees(this.rotationMainX.getValueFloat()));
         event.getStack().mulPose(Axis.YP.rotationDegrees(this.rotationMainY.getValueFloat()));
         event.getStack().mulPose(Axis.ZP.rotationDegrees(this.rotationMainZ.getValueFloat()));
      } else {
         event.getStack().translate(this.positionOffX.getValueFloat(), this.positionOffY.getValueFloat(), this.positionOffZ.getValueFloat());
         event.getStack().scale(this.scaleOffX.getValueFloat(), this.scaleOffY.getValueFloat(), this.scaleOffZ.getValueFloat());
         event.getStack().mulPose(Axis.XP.rotationDegrees(this.rotationOffX.getValueFloat()));
         event.getStack().mulPose(Axis.YP.rotationDegrees(this.rotationOffY.getValueFloat()));
         event.getStack().mulPose(Axis.ZP.rotationDegrees(this.rotationOffZ.getValueFloat()));
      }
   }
}
