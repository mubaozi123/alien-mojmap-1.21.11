package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.asm.accessors.IMinecraftClient;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.event.KeyEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTool extends Module {
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting fish = this.add(new BooleanSetting("Fish", true).setParent());
   private final BooleanSetting autoCast = this.add(new BooleanSetting("AutoCast", true, this.fish::isOpen));
   private final SliderSetting ticksAutoCast = this.add(new SliderSetting("TicksAutoCast", 10, 0, 60, this.fish::isOpen));
   private final SliderSetting ticksCatch = this.add(new SliderSetting("TicksCatch", 6, 0, 60, this.fish::isOpen));
   private final SliderSetting ticksThrow = this.add(new SliderSetting("TicksThrow", 14, 0, 60, this.fish::isOpen));
   private final BooleanSetting splashDetection = this.add(new BooleanSetting("SplashDetection", false, this.fish::isOpen));
   private final SliderSetting splashDetectionRange = this.add(new SliderSetting("DetectionRange", 10, 0, 60, this.fish::isOpen));
   private boolean ticksEnabled;
   private int ticksToRightClick;
   private int ticksData;
   private int autoCastTimer;
   private boolean autoCastEnabled;
   private int autoCastCheckTimer;

   public AutoTool() {
      super("AutoTool", Module.Category.Player);
      this.setChinese("自动工具");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.autoFish();
      this.autoTool();
   }

   public void autoTool() {
      if (this.mine.getValue()) {
         if (mc.hitResult instanceof BlockHitResult result) {
            BlockPos pos = result.getBlockPos();
            if (!mc.level.isEmptyBlock(pos)) {
               int tool = getTool(pos);
               if (tool != -1 && mc.options.keyAttack.isDown()) {
                  mc.player.getInventory().selected = tool;
               }
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.ticksEnabled = false;
      this.autoCastEnabled = false;
      this.autoCastCheckTimer = 0;
   }

   @EventListener
   private void onPlaySound(PlaySoundEvent event) {
      if (!nullCheck()) {
         if (this.fish.getValue()) {
            SoundInstance p = event.sound;
            FishingHook elementCodec = mc.player.fishing;
            if (elementCodec != null) {
               if (p.getIdentifier().getPath().equals("entity.fishing_bobber.splash")
                  && (
                     !this.splashDetection.getValue()
                        || MathUtil.distance(elementCodec.getX(), elementCodec.getY(), elementCodec.getZ(), p.getX(), p.getY(), p.getZ())
                           <= this.splashDetectionRange.getValue()
                  )) {
                  this.ticksEnabled = true;
                  this.ticksToRightClick = this.ticksCatch.getValueInt();
                  this.ticksData = 0;
               }
            }
         }
      }
   }

   public void autoFish() {
      if (this.fish.getValue()) {
         if (this.autoCastCheckTimer <= 0) {
            this.autoCastCheckTimer = 30;
            if (this.autoCast.getValue()
               && !this.ticksEnabled
               && !this.autoCastEnabled
               && mc.player.fishing == null
               && mc.player.getMainHandItem().getItem() == Items.FISHING_ROD) {
               this.autoCastTimer = 0;
               this.autoCastEnabled = true;
            }
         } else {
            this.autoCastCheckTimer--;
         }

         if (this.autoCastEnabled) {
            this.autoCastTimer++;
            if (this.autoCastTimer > this.ticksAutoCast.getValue()) {
               this.autoCastEnabled = false;
               ((IMinecraftClient)mc).invokeDoItemUse();
            }
         }

         if (this.ticksEnabled && this.ticksToRightClick <= 0) {
            if (this.ticksData == 0) {
               ((IMinecraftClient)mc).invokeDoItemUse();
               this.ticksToRightClick = this.ticksThrow.getValueInt();
               this.ticksData = 1;
            } else if (this.ticksData == 1) {
               ((IMinecraftClient)mc).invokeDoItemUse();
               this.ticksEnabled = false;
            }
         }

         this.ticksToRightClick--;
      }
   }

   @EventListener
   private void onKey(KeyEvent event) {
      if (mc.options.keyUse.isDown()) {
         this.ticksEnabled = false;
      }
   }

   public static int getTool(BlockPos pos) {
      int index = -1;
      float CurrentFastest = 1.0F;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != ItemStack.EMPTY) {
            float digSpeed = EnchantmentHelper.getItemEnchantmentLevel(
               mc.level.registryAccess().lookupOrThrow(Enchantments.EFFICIENCY.registryKey()).getOrThrow(Enchantments.EFFICIENCY), stack
            );
            float destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos));
            if (mc.level.getBlockState(pos).getBlock() instanceof AirBlock) {
               return -1;
            }

            if (digSpeed + destroySpeed > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }
      }

      return index;
   }
}
