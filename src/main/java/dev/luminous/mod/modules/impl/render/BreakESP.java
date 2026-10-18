package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BreakESP extends Module {
   public static BreakESP INSTANCE;
   private final BooleanSetting progress = this.add(new BooleanSetting("Progress", true));
   private final SliderSetting damage = this.add(new SliderSetting("Damage", 1.0, 0.0, 2.0, 0.01));
   private final ColorSetting box = this.add(new ColorSetting("Box", new Color(198, 176, 12, 255)).injectBoolean(true));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(198, 176, 12, 78)).injectBoolean(true));
   private final ColorSetting boxFriend = this.add(new ColorSetting("FriendBox", new Color(30, 45, 169, 255)).injectBoolean(true));
   private final ColorSetting fillFriend = this.add(new ColorSetting("FriendFill", new Color(30, 45, 169, 78)).injectBoolean(true));
   private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut));
   private final BooleanSetting second = this.add(new BooleanSetting("Second", true));
   private final ColorSetting secondBox = this.add(new ColorSetting("SecondBox", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting secondFill = this.add(new ColorSetting("SecondFill", new Color(255, 255, 255, 100)).injectBoolean(true));
   final DecimalFormat df = new DecimalFormat("0.0");
   final Color startColor = new Color(255, 6, 6);
   final Color endColor = new Color(0, 255, 12);
   final Color doubleColor = new Color(255, 179, 96);

   public BreakESP() {
      super("BreakESP", Module.Category.Render);
      this.setChinese("挖掘显示");
      INSTANCE = this;
   }

   private Color getFillColor(Player player) {
      return Alien.FRIEND.isFriend(player) ? this.fillFriend.getValue() : this.fill.getValue();
   }

   private Color getBoxColor(Player player) {
      return Alien.FRIEND.isFriend(player) ? this.boxFriend.getValue() : this.box.getValue();
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      for (BreakManager.BreakData breakData : Alien.BREAK.breakMap.values()) {
         if (breakData != null && breakData.getEntity() != null) {
            Player player = (Player)breakData.getEntity();
            double size = 0.5 * (1.0 - breakData.fade.ease(this.ease.getValue()));
            AABB cbox = new AABB(breakData.pos).contract(size, size, size).contract(-size, -size, -size);
            if (this.fill.booleanValue) {
               Render3DUtil.drawFill(matrixStack, cbox, this.getFillColor(player));
            }

            if (this.box.booleanValue) {
               Render3DUtil.drawBox(matrixStack, cbox, this.getBoxColor(player));
            }

            Render3DUtil.drawText3D(player.getName().getString(), breakData.pos.getCenter().add(0.0, this.progress.getValue() ? 0.15 : 0.0, 0.0), -1);
            if (this.progress.getValue()) {
               Render3DUtil.drawText3D(
                  Component.nullToEmpty(
                     breakData.failed
                        ? "§4Failed"
                        : (breakData.complete ? "Broke" : this.df.format(Math.min(1.0, breakData.timer.getMs() / breakData.breakTime) * 100.0))
                  ),
                  breakData.pos.getCenter().add(0.0, -0.15, 0.0),
                  0.0,
                  0.0,
                  1.0,
                  breakData.complete
                     ? (mc.level.isEmptyBlock(breakData.pos) ? this.endColor : this.startColor)
                     : ColorUtil.fadeColor(this.startColor, this.endColor, breakData.timer.getMs() / breakData.breakTime)
               );
            }
         }
      }

      if (this.second.getValue()) {
         for (int i : Alien.BREAK.doubleMap.keySet()) {
            BreakManager.BreakData breakDatax = Alien.BREAK.doubleMap.get(i);
            if (breakDatax != null && breakDatax.getEntity() != null && !mc.level.isEmptyBlock(breakDatax.pos)) {
               BreakManager.BreakData singleBreakData = Alien.BREAK.breakMap.get(i);
               if (singleBreakData == null || !singleBreakData.pos.equals(breakDatax.pos)) {
                  double sizex = 0.5 * (1.0 - breakDatax.fade.ease(this.ease.getValue()));
                  AABB cboxx = new AABB(breakDatax.pos).contract(sizex, sizex, sizex).contract(-sizex, -sizex, -sizex);
                  if (this.secondFill.booleanValue) {
                     Render3DUtil.drawFill(matrixStack, cboxx, this.secondFill.getValue());
                  }

                  if (this.secondBox.booleanValue) {
                     Render3DUtil.drawBox(matrixStack, cboxx, this.secondBox.getValue());
                  }

                  Render3DUtil.drawText3D(breakDatax.getEntity().getName().getString(), breakDatax.pos.getCenter().add(0.0, 0.15, 0.0), -1);
                  Render3DUtil.drawText3D("Double", breakDatax.pos.getCenter().add(0.0, -0.15, 0.0), this.doubleColor.getRGB());
               }
            } else {
               Alien.BREAK.doubleMap.remove(i);
            }
         }
      }
   }

   public static double getBreakTime(BlockPos pos, boolean extraBreak) {
      int slot = getTool(pos);
      if (slot == -1) {
         slot = mc.player.getInventory().selected;
      }

      return getBreakTime(pos, slot, extraBreak ? 1.0 : INSTANCE.damage.getValue());
   }

   static int getTool(BlockPos pos) {
      AtomicInteger slot = new AtomicInteger();
      slot.set(-1);
      float CurrentFastest = 1.0F;

      for (Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
         float destroySpeed;
         float digSpeed;
         if (!(entry.getValue().getItem() instanceof AirItem)
            && (
                     digSpeed = EnchantmentHelper.getItemEnchantmentLevel(
                        mc.level.registryAccess().lookupOrThrow(Enchantments.EFFICIENCY.registryKey()).getOrThrow(Enchantments.EFFICIENCY), entry.getValue()
                     )
                  )
                  + (destroySpeed = entry.getValue().getDestroySpeed(mc.level.getBlockState(pos)))
               > CurrentFastest) {
            CurrentFastest = digSpeed + destroySpeed;
            slot.set(entry.getKey());
         }
      }

      return slot.get();
   }

   static double getBreakTime(BlockPos pos, int slot, double damage) {
      return 1.0F / getBlockStrength(pos, mc.player.getInventory().getItem(slot)) / 20.0F * 1000.0F * damage;
   }

   static float getBlockStrength(BlockPos position, ItemStack itemStack) {
      BlockState state = mc.level.getBlockState(position);
      float hardness = state.getDestroySpeed(mc.level, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float i = state.requiresCorrectToolForDrops() && !itemStack.isCorrectToolForDrops(state) ? 100.0F : 30.0F;
         return getDigSpeed(state, itemStack) / hardness / i;
      }
   }

   static float getDigSpeed(BlockState state, ItemStack itemStack) {
      float digSpeed = getDestroySpeed(state, itemStack);
      int efficiencyModifier;
      if (digSpeed > 1.0F
         && (
               efficiencyModifier = EnchantmentHelper.getItemEnchantmentLevel(
                  mc.level.registryAccess().lookupOrThrow(Enchantments.EFFICIENCY.registryKey()).getOrThrow(Enchantments.EFFICIENCY), itemStack
               )
            )
            > 0
         && !itemStack.isEmpty()) {
         digSpeed += (float)(StrictMath.pow(efficiencyModifier, 2.0) + 1.0);
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }

   static float getDestroySpeed(BlockState state, ItemStack itemStack) {
      float destroySpeed = 1.0F;
      if (itemStack != null && !itemStack.isEmpty()) {
         destroySpeed *= itemStack.getDestroySpeed(state);
      }

      return destroySpeed;
   }
}
