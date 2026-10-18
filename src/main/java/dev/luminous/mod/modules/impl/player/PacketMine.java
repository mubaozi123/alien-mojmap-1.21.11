package dev.luminous.mod.modules.impl.player;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClickBlockEvent;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.CevBreaker;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PacketMine extends Module {
   public static PacketMine INSTANCE;
   public static BlockPos secondPos;
   public static double progress = 0.0;
   private final FadeUtils animationTime = new FadeUtils(1000L);
   private final FadeUtils secondAnim = new FadeUtils(1000L);
   private final DecimalFormat df = new DecimalFormat("0.0");
   private final EnumSetting<PacketMine.Page> page = this.add(new EnumSetting<>("Page", PacketMine.Page.General));
   private final SliderSetting stopDelay = this.add(new SliderSetting("StopDelay", 50.0, 0.0, 500.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
   private final SliderSetting startDelay = this.add(new SliderSetting("StartDelay", 200.0, 0.0, 500.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
   private final SliderSetting damage = this.add(new SliderSetting("Damage", 0.7F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General)));
   private final SliderSetting maxBreak = this.add(new SliderSetting("MaxBreak", 3.0, 0.0, 20.0, 1.0, () -> this.page.is(PacketMine.Page.General)));
   public final BooleanSetting noGhostHand = this.add(new BooleanSetting("1.21", false, () -> this.page.is(PacketMine.Page.General)));
   public final BooleanSetting noCollide = this.add(new BooleanSetting("NoCollide", true, () -> this.page.is(PacketMine.Page.General)));
   private final EnumSetting<Timing> timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == PacketMine.Page.General));
   private final BooleanSetting grimDisabler = this.add(new BooleanSetting("GrimDisabler", false, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting instant = this.add(new BooleanSetting("Instant", false, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting wait = this.add(new BooleanSetting("Wait", true, () -> !this.instant.getValue() && this.page.is(PacketMine.Page.General)));
   private final BooleanSetting mineAir = this.add(
      new BooleanSetting("MineAir", true, () -> this.wait.getValue() && !this.instant.getValue() && this.page.is(PacketMine.Page.General))
   );
   private final BooleanSetting hotBar = this.add(new BooleanSetting("HotbarSwap", false, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting doubleBreak = this.add(new BooleanSetting("DoubleBreak", true, () -> this.page.is(PacketMine.Page.General))).setParent();
   public final BooleanSetting autoSwitch = this.add(
      new BooleanSetting("AutoSwitch", true, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen())
   );
   private final SliderSetting start = this.add(
      new SliderSetting("Start", 0.9F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen())
   );
   private final SliderSetting timeOut = this.add(
      new SliderSetting("TimeOut", 1.2F, 0.0, 2.0, 0.01, () -> this.page.is(PacketMine.Page.General) && this.doubleBreak.isOpen())
   );
   private final BooleanSetting setAir = this.add(new BooleanSetting("SetAir", false, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting swing = this.add(new BooleanSetting("Swing", true, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting endSwing = this.add(new BooleanSetting("EndSwing", false, () -> this.page.is(PacketMine.Page.General)));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 3.0, 10.0, 0.1, () -> this.page.is(PacketMine.Page.General)));
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<>("SwingSide", SwingSide.All, () -> this.page.is(PacketMine.Page.General)));
   private final BooleanSetting unbreakableCancel = this.add(new BooleanSetting("UnbreakableCancel", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting switchReset = this.add(new BooleanSetting("SwitchReset", false, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting preferWeb = this.add(new BooleanSetting("PreferWeb", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting preferHead = this.add(new BooleanSetting("PreferHead", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting farCancel = this.add(new BooleanSetting("FarCancel", false, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting checkWeb = this.add(new BooleanSetting("CheckWeb", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting checkGround = this.add(new BooleanSetting("CheckGround", true, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.page.is(PacketMine.Page.Check) && this.checkGround.getValue()));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false, () -> this.page.is(PacketMine.Page.Check)).setParent());
   private final BooleanSetting allowOffhand = this.add(
      new BooleanSetting("AllowOffhand", true, () -> this.page.is(PacketMine.Page.Check) && this.usingPause.isOpen())
   );
   private final BooleanSetting bypassGround = this.add(new BooleanSetting("BypassGround", true, () -> this.page.is(PacketMine.Page.Check)));
   private final SliderSetting bypassTime = this.add(
      new SliderSetting("BypassTime", 400, 0, 2000, () -> this.bypassGround.getValue() && this.page.is(PacketMine.Page.Check))
   );
   private final BindSetting pause = this.add(new BindSetting("Pause", -1, () -> this.page.is(PacketMine.Page.Check)));
   private final BooleanSetting rotate = this.add(new BooleanSetting("StartRotate", true, () -> this.page.is(PacketMine.Page.Rotation)));
   private final BooleanSetting endRotate = this.add(new BooleanSetting("EndRotate", false, () -> this.page.is(PacketMine.Page.Rotation)));
   private final SliderSetting syncTime = this.add(new SliderSetting("Sync", 300, 0, 1000, () -> this.page.is(PacketMine.Page.Rotation)));
   private final BooleanSetting yawStep = this.add(new BooleanSetting("YawStep", false, () -> this.page.is(PacketMine.Page.Rotation)).setParent());
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen())
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen())
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen())
   );
   private final SliderSetting fov = this.add(
      new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen())
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.page.is(PacketMine.Page.Rotation) && this.yawStep.isOpen())
   );
   public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", false, () -> this.page.is(PacketMine.Page.Place)).setParent());
   private final BooleanSetting onlyHeadBomber = this.add(
      new BooleanSetting("OnlyCev", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen())
   );
   private final BooleanSetting waitPlace = this.add(new BooleanSetting("WaitPlace", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
   private final BooleanSetting spamPlace = this.add(new BooleanSetting("SpamPlace", false, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen()));
   private final BooleanSetting afterBreak = this.add(
      new BooleanSetting("AfterBreak", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen())
   );
   private final BooleanSetting checkDamage = this.add(
      new BooleanSetting("DetectProgress", true, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen())
   );
   private final SliderSetting crystalDamage = this.add(
      new SliderSetting("Progress", 0.9F, 0.0, 1.0, 0.01, () -> this.page.is(PacketMine.Page.Place) && this.crystal.isOpen() && this.checkDamage.getValue())
   );
   public final BindSetting obsidian = this.add(new BindSetting("Obsidian", -1, () -> this.page.is(PacketMine.Page.Place)));
   private final BindSetting enderChest = this.add(new BindSetting("EnderChest", -1, () -> this.page.is(PacketMine.Page.Place)));
   private final BooleanSetting placeRotate = this.add(new BooleanSetting("PlaceRotate", true, () -> this.page.is(PacketMine.Page.Place)));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, () -> this.page.is(PacketMine.Page.Place)));
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 100, 0, 1000, () -> this.page.is(PacketMine.Page.Place)));
   private final BooleanSetting checkDouble = this.add(new BooleanSetting("CheckDouble", false, () -> this.page.is(PacketMine.Page.Render)));
   private final EnumSetting<PacketMine.Animation> animation = this.add(
      new EnumSetting<>("Animation", PacketMine.Animation.Up, () -> this.page.is(PacketMine.Page.Render))
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
   private final EnumSetting<Easing> fadeEase = this.add(new EnumSetting<>("FadeEase", Easing.CubicInOut, () -> this.page.is(PacketMine.Page.Render)));
   private final SliderSetting expandLine = this.add(new SliderSetting("ExpandLine", 0.0, 0.0, 1.0, () -> this.page.is(PacketMine.Page.Render)));
   private final ColorSetting startColor = this.add(new ColorSetting("StartFill", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
   private final ColorSetting startOutlineColor = this.add(
      new ColorSetting("StartOutline", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render))
   );
   private final ColorSetting endColor = this.add(new ColorSetting("EndFill", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render)));
   private final ColorSetting endOutlineColor = this.add(
      new ColorSetting("EndOutline", new Color(255, 255, 255, 100), () -> this.page.is(PacketMine.Page.Render))
   );
   private final ColorSetting doubleColor = this.add(
      new ColorSetting("DoubleFill", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render))
   );
   private final ColorSetting doubleOutlineColor = this.add(
      new ColorSetting("DoubleOutline", new Color(88, 94, 255, 100), () -> this.doubleBreak.getValue() && this.page.is(PacketMine.Page.Render))
   );
   private final BooleanSetting text = this.add(new BooleanSetting("Text", true, () -> this.page.is(PacketMine.Page.Render)));
   private final BooleanSetting box = this.add(new BooleanSetting("Box", true, () -> this.page.is(PacketMine.Page.Render)));
   private final BooleanSetting outline = this.add(new BooleanSetting("Outline", true, () -> this.page.is(PacketMine.Page.Render)));
   private final Timer mineTimer = new Timer();
   private final Timer sync = new Timer();
   private final Timer secondTimer = new Timer();
   private final Timer delayTimer = new Timer();
   private final Timer placeTimer = new Timer();
   private final Timer startTime = new Timer();
   public static boolean ghost = false;
   public static boolean complete = false;
   int lastSlot = -1;
   Vec3 directionVec = null;
   Runnable switchBack;
   BlockPos breakPos;
   boolean startPacket = false;
   int breakNumber = 0;
   double breakFinalTime;
   double secondFinalTime;
   boolean sendGroundPacket = false;
   boolean swapped = false;
   int mainSlot = 0;

   public PacketMine() {
      super("PacketMine", Module.Category.Player);
      this.setChinese("发包挖掘");
      INSTANCE = this;
   }

   public static BlockPos getBreakPos() {
      return INSTANCE.isOn() ? INSTANCE.breakPos : null;
   }

   @Override
   public String getInfo() {
      return progress >= 1.0 ? "Done" : this.df.format(progress * 100.0) + "%";
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (this.rotate.getValue() && this.shouldYawStep() && this.directionVec != null && !this.sync.passedMs(this.syncTime.getValue())) {
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @Override
   public void onLogin() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
      secondPos = null;
   }

   @Override
   public void onDisable() {
      this.startPacket = false;
      ghost = false;
      complete = false;
      this.breakPos = null;
   }

   private void autoSwitch() {
      if (this.autoSwitch.getValue() && this.doubleBreak.getValue()) {
         int index = -1;
         if (secondPos != null) {
            float CurrentFastest = 1.0F;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               float destroySpeed;
               float digSpeed;
               if (stack != ItemStack.EMPTY
                  && (
                           digSpeed = EnchantmentHelper.getItemEnchantmentLevel(
                              mc.level.registryAccess().lookupOrThrow(Enchantments.EFFICIENCY.registryKey()).getOrThrow(Enchantments.EFFICIENCY), stack
                           )
                        )
                        + (destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(secondPos)))
                     > CurrentFastest) {
                  CurrentFastest = digSpeed + destroySpeed;
                  index = i;
               }
            }
         }

         if (index != -1
            && !mc.options.keyUse.isDown()
            && !mc.options.keyAttack.isDown()
            && !mc.player.isUsingItem()
            && this.secondTimer.passedMs(this.getBreakTime(secondPos, index, this.start.getValue()))) {
            if (index != mc.player.getInventory().selected) {
               this.mainSlot = mc.player.getInventory().selected;
               InventoryUtil.switchToSlot(index);
               this.swapped = true;
            }
         } else if (this.swapped) {
            InventoryUtil.switchToSlot(this.mainSlot);
            this.swapped = false;
         }
      }
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.breakPos != null && mc.level.isEmptyBlock(this.breakPos)) {
            complete = true;
         }

         if (secondPos != null) {
            int secondSlot = this.getTool(secondPos);
            if (secondSlot == -1) {
               secondSlot = mc.player.getInventory().selected;
            }

            this.secondFinalTime = this.getBreakTime(secondPos, secondSlot, 1.0);
            if (!this.isAir(secondPos) && !unbreakable(secondPos)) {
               double time = this.getBreakTime(secondPos, mc.player.getInventory().selected, 1.0);
               if (this.secondTimer.passedMs(time * this.timeOut.getValue())) {
                  secondPos = null;
               }
            } else {
               secondPos = null;
            }
         }

         if (this.switchBack != null && event.isPre()) {
            this.switchBack.run();
            this.switchBack = null;
         }

         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (mc.player.isDeadOrDying()) {
               secondPos = null;
            }

            this.autoSwitch();
            if (mc.player.isCreative()) {
               this.startPacket = false;
               ghost = false;
               complete = false;
               this.breakNumber = 0;
               this.breakPos = null;
               progress = 0.0;
            } else if (this.breakPos == null) {
               this.breakNumber = 0;
               this.startPacket = false;
               ghost = false;
               complete = false;
               progress = 0.0;
            } else {
               int slot = this.getTool(this.breakPos);
               if (slot == -1) {
                  slot = mc.player.getInventory().selected;
               }

               this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
               progress = this.mineTimer.getMs() / this.breakFinalTime;
               if (this.isAir(this.breakPos)) {
                  this.breakNumber = 0;
               }

               if ((!(this.breakNumber > this.maxBreak.getValue() - 1.0) || !(this.maxBreak.getValue() > 0.0) || complete)
                  && (this.wait.getValue() || !this.isAir(this.breakPos) || this.instant.getValue())) {
                  if (unbreakable(this.breakPos)) {
                     if (this.unbreakableCancel.getValue()) {
                        this.breakPos = null;
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                     }

                     this.breakNumber = 0;
                  } else if (Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(this.breakPos.getCenter())) > this.range.getValue()) {
                     if (this.farCancel.getValue()) {
                        this.startPacket = false;
                        ghost = false;
                        complete = false;
                        this.breakNumber = 0;
                        this.breakPos = null;
                     }
                  } else if (!this.usingPause.getValue()
                     || !mc.player.isUsingItem()
                     || this.allowOffhand.getValue() && mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
                     if (!this.pause.isPressed()) {
                        if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                           if (!this.breakPos.equals(AutoAnchor.INSTANCE.currentPos) || !(BlockUtil.getBlock(getBreakPos()) instanceof RespawnAnchorBlock)) {
                              if (this.hotBar.getValue() || EntityUtil.inInventory()) {
                                 if (this.isAir(this.breakPos)) {
                                    if (this.shouldCrystal()) {
                                       for (Direction facing : Direction.values()) {
                                          CombatUtil.attackCrystal(this.breakPos.relative(facing), this.placeRotate.getValue(), true);
                                       }
                                    }

                                    if (this.placeTimer.passedMs(this.placeDelay.getValue()) && BlockUtil.canPlace(this.breakPos) && mc.screen == null) {
                                       if (this.enderChest.isPressed()) {
                                          int eChest = this.findBlock(Blocks.ENDER_CHEST);
                                          if (eChest != -1) {
                                             int oldSlot = mc.player.getInventory().selected;
                                             this.doSwap(eChest, eChest);
                                             BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                             this.doSwap(oldSlot, eChest);
                                             this.placeTimer.reset();
                                          }
                                       } else if (this.obsidian.isPressed()) {
                                          int obsidian = this.findBlock(Blocks.OBSIDIAN);
                                          if (obsidian != -1) {
                                             boolean hasCrystal = false;
                                             if (this.shouldCrystal()) {
                                                for (Entity entity : BlockUtil.getEntities(new AABB(this.breakPos.above()))) {
                                                   if (entity instanceof EndCrystal) {
                                                      hasCrystal = true;
                                                      break;
                                                   }
                                                }
                                             }

                                             if (!hasCrystal || this.spamPlace.getValue()) {
                                                int oldSlot = mc.player.getInventory().selected;
                                                this.doSwap(obsidian, obsidian);
                                                BlockUtil.placeBlock(this.breakPos, this.placeRotate.getValue(), true);
                                                this.doSwap(oldSlot, obsidian);
                                                this.placeTimer.reset();
                                             }
                                          }
                                       }
                                    }

                                    this.breakNumber = 0;
                                 } else if (this.canPlaceCrystal(this.breakPos.above()) && this.shouldCrystal()) {
                                    if (this.placeTimer.passedMs(this.placeDelay.getValue())) {
                                       if (this.checkDamage.getValue()) {
                                          if (this.mineTimer.getMs() / this.breakFinalTime >= this.crystalDamage.getValue() && !this.placeCrystal()) {
                                             return;
                                          }
                                       } else if (!this.placeCrystal()) {
                                          return;
                                       }
                                    } else if (this.startPacket) {
                                       return;
                                    }
                                 }

                                 if (this.waitPlace.getValue()) {
                                    for (Direction i : Direction.values()) {
                                       if (this.breakPos.relative(i).equals(AutoCrystal.INSTANCE.crystalPos)) {
                                          if (AutoCrystal.INSTANCE.canPlaceCrystal(this.breakPos, false, false)) {
                                             return;
                                          }
                                          break;
                                       }
                                    }
                                 }

                                 if (this.delayTimer.passed((long)this.stopDelay.getValue())) {
                                    if (this.startPacket) {
                                       if (this.isAir(this.breakPos)) {
                                          return;
                                       }

                                       if (this.onlyGround.getValue() && !mc.player.onGround()) {
                                          return;
                                       }

                                       if (this.mineTimer.passed((long)this.breakFinalTime)) {
                                          if (this.endRotate.getValue()
                                             && this.shouldYawStep()
                                             && !this.faceVector(this.breakPos.getCenter().relative(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                                             return;
                                          }

                                          int old = mc.player.getInventory().selected;
                                          boolean shouldSwitch;
                                          if (this.hotBar.getValue()) {
                                             shouldSwitch = slot != old;
                                          } else {
                                             if (slot < 9) {
                                                slot += 36;
                                             }

                                             shouldSwitch = old + 36 != slot;
                                          }

                                          if (shouldSwitch) {
                                             if (this.hotBar.getValue()) {
                                                InventoryUtil.switchToSlot(slot);
                                             } else {
                                                mc.gameMode
                                                   .handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, old, ClickType.SWAP, mc.player);
                                             }
                                          }

                                          int finalSlot = slot;
                                          this.switchBack = () -> {
                                             if (this.endRotate.getValue()
                                                && !this.faceVector(this.breakPos.getCenter().relative(BlockUtil.getClickSide(this.breakPos), 0.5))) {
                                                if (shouldSwitch) {
                                                   if (this.hotBar.getValue()) {
                                                      InventoryUtil.switchToSlot(old);
                                                   } else {
                                                      mc.gameMode
                                                         .handleInventoryMouseClick(
                                                            mc.player.containerMenu.containerId, finalSlot, old, ClickType.SWAP, mc.player
                                                         );
                                                      EntityUtil.syncInventory();
                                                   }
                                                }
                                             } else {
                                                sendSequencedPacket(
                                                   id -> new ServerboundPlayerActionPacket(
                                                      Action.STOP_DESTROY_BLOCK, this.breakPos, BlockUtil.getClickSide(this.breakPos), id
                                                   )
                                                );
                                                if (this.endSwing.getValue()) {
                                                   EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.swingMode.getValue());
                                                }

                                                if (shouldSwitch) {
                                                   if (this.hotBar.getValue()) {
                                                      InventoryUtil.switchToSlot(old);
                                                   } else {
                                                      mc.gameMode
                                                         .handleInventoryMouseClick(
                                                            mc.player.containerMenu.containerId, finalSlot, old, ClickType.SWAP, mc.player
                                                         );
                                                      EntityUtil.syncInventory();
                                                   }
                                                }

                                                this.breakNumber++;
                                                this.delayTimer.reset();
                                                this.startTime.reset();
                                                if (this.afterBreak.getValue() && this.shouldCrystal()) {
                                                   for (Direction facing : Direction.values()) {
                                                      CombatUtil.attackCrystal(this.breakPos.relative(facing), this.placeRotate.getValue(), true);
                                                   }
                                                }

                                                if (this.setAir.getValue()) {
                                                   mc.level.setBlockAndUpdate(this.breakPos, Blocks.AIR.defaultBlockState());
                                                }

                                                if (this.endRotate.getValue() && !this.shouldYawStep()) {
                                                   Alien.ROTATION.snapBack();
                                                }

                                                ghost = true;
                                             }
                                          };
                                          if (!this.noGhostHand.getValue()) {
                                             this.switchBack.run();
                                             this.switchBack = null;
                                          }
                                       }
                                    } else {
                                       if (!this.startTime.passed(this.startDelay.getValueInt())) {
                                          return;
                                       }

                                       if (!this.mineAir.getValue() && this.isAir(this.breakPos)) {
                                          return;
                                       }

                                       Direction side = BlockUtil.getClickSide(this.breakPos);
                                       if (this.rotate.getValue()) {
                                          Vec3i vec3i = side.getUnitVec3i();
                                          if (!this.faceVector(
                                             this.breakPos.getCenter().add(new Vec3(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5))
                                          )) {
                                             return;
                                          }
                                       }

                                       this.mineTimer.reset();
                                       this.animationTime.reset();
                                       if (this.swing.getValue()) {
                                          EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.swingMode.getValue());
                                       }

                                       if (this.doubleBreak.getValue()) {
                                          if (secondPos == null || this.isAir(secondPos)) {
                                             double breakTime = this.getBreakTime(this.breakPos, slot, 1.0);
                                             this.secondAnim.reset();
                                             this.secondAnim.setLength((long)breakTime);
                                             this.secondTimer.reset();
                                             secondPos = this.breakPos;
                                          }

                                          this.doDoubleBreak(side);
                                       }

                                       sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                       if (this.rotate.getValue() && !this.shouldYawStep()) {
                                          Alien.ROTATION.snapBack();
                                       }

                                       this.startTime.reset();
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               } else {
                  if (this.breakPos.equals(secondPos)) {
                     secondPos = null;
                  }

                  this.startPacket = false;
                  ghost = false;
                  complete = false;
                  this.breakNumber = 0;
                  this.breakPos = null;
               }
            }
         }
      }
   }

   private void breakBlock(BlockPos breakPos) {
      mc.level.getBlockState(breakPos).getBlock().playerWillDestroy(mc.level, breakPos, mc.level.getBlockState(breakPos), mc.player);
   }

   void doDoubleBreak(Direction side) {
      sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
      sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, this.breakPos, side, id));
   }

   boolean placeCrystal() {
      int crystal = this.findCrystal();
      if (crystal != -1) {
         int oldSlot = mc.player.getInventory().selected;
         this.doSwap(crystal, crystal);
         BlockUtil.placeCrystal(this.breakPos.above(), this.placeRotate.getValue());
         this.doSwap(oldSlot, crystal);
         this.placeTimer.reset();
         return !this.waitPlace.getValue();
      } else {
         return true;
      }
   }

   @EventListener
   public void onAttackBlock(ClickBlockEvent event) {
      if (!nullCheck()) {
         if (!mc.player.isCreative()) {
            event.cancel();
            BlockPos pos = event.getPos();
            if (!pos.equals(this.breakPos)) {
               if (!unbreakable(pos)) {
                  if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                     if (this.breakPos == null
                        || !this.preferHead.getValue()
                        || !mc.player.isVisuallyCrawling()
                        || !EntityUtil.getPlayerPos(true).above().equals(this.breakPos)) {
                        if (BlockUtil.getClickSideStrict(pos) != null) {
                           if (!(Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter())) > this.range.getValue())) {
                              this.breakPos = pos;
                              this.breakNumber = 0;
                              this.startPacket = false;
                              ghost = false;
                              complete = false;
                              this.mineTimer.reset();
                              this.animationTime.reset();
                              if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                                 Direction side = BlockUtil.getClickSide(this.breakPos);
                                 if (this.rotate.getValue()) {
                                    Vec3i vec3i = side.getUnitVec3i();
                                    if (!this.faceVector(this.breakPos.getCenter().add(new Vec3(vec3i.getX() * 0.5, vec3i.getY() * 0.5, vec3i.getZ() * 0.5)))) {
                                       return;
                                    }
                                 }

                                 if (this.startTime.passed(this.startDelay.getValueInt())) {
                                    if (this.swing.getValue()) {
                                       EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.swingMode.getValue());
                                    }

                                    if (this.doubleBreak.getValue()) {
                                       if (secondPos == null || this.isAir(secondPos)) {
                                          int slot = this.getTool(this.breakPos);
                                          if (slot == -1) {
                                             slot = mc.player.getInventory().selected;
                                          }

                                          this.secondFinalTime = this.getBreakTime(this.breakPos, slot, 1.0);
                                          this.secondAnim.reset();
                                          this.secondAnim.setLength((long)this.secondFinalTime);
                                          this.secondTimer.reset();
                                          secondPos = this.breakPos;
                                       }

                                       this.doDoubleBreak(side);
                                    }

                                    int slot = this.getTool(this.breakPos);
                                    if (slot == -1) {
                                       slot = mc.player.getInventory().selected;
                                    }

                                    this.breakFinalTime = this.getBreakTime(this.breakPos, slot);
                                    sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.breakPos, side, id));
                                    if (this.rotate.getValue() && !this.shouldYawStep()) {
                                       Alien.ROTATION.snapBack();
                                    }

                                    this.startTime.reset();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void mine(BlockPos pos) {
      if (!nullCheck()) {
         if (mc.player.isCreative()) {
            mc.gameMode.startDestroyBlock(pos, BlockUtil.getClickSide(pos));
         } else if (this.isOff()) {
            mc.gameMode.startDestroyBlock(pos, BlockUtil.getClickSide(pos));
         } else if (!pos.equals(this.breakPos)) {
            if (!unbreakable(pos)) {
               if (this.breakPos == null || !this.preferWeb.getValue() || BlockUtil.getBlock(this.breakPos) != Blocks.COBWEB) {
                  if (this.breakPos == null
                     || !this.preferHead.getValue()
                     || !mc.player.isVisuallyCrawling()
                     || !EntityUtil.getPlayerPos(true).above().equals(this.breakPos)) {
                     if (BlockUtil.getClickSideStrict(pos) != null) {
                        if (!(Mth.sqrt((float)mc.player.getEyePosition().distanceToSqr(pos.getCenter())) > this.range.getValue())) {
                           this.breakPos = pos;
                           this.breakNumber = 0;
                           this.startPacket = false;
                           ghost = false;
                           complete = false;
                           this.mineTimer.reset();
                           this.animationTime.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   boolean faceVector(Vec3 directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.sync.reset();
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (this.breakPos != null && mc.level.isEmptyBlock(this.breakPos)) {
         complete = true;
      }

      if (!mc.player.isCreative()) {
         if (secondPos != null) {
            if (this.isAir(secondPos)) {
               secondPos = null;
               return;
            }

            if (!this.checkDouble.getValue() || !secondPos.equals(this.breakPos)) {
               this.secondAnim.setLength((long)this.secondFinalTime);
               double ease = this.secondAnim.ease(this.ease.getValue());
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(matrixStack, this.getFillBox(secondPos, ease), this.doubleColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(matrixStack, this.getOutlineBox(secondPos, ease), this.doubleOutlineColor.getValue());
               }
            }
         }

         if (this.breakPos != null) {
            progress = this.mineTimer.getMs() / this.breakFinalTime;
            this.animationTime.setLength((long)this.breakFinalTime);
            double easex = this.animationTime.ease(this.ease.getValue());
            if (unbreakable(this.breakPos)) {
               if (this.box.getValue()) {
                  Render3DUtil.drawFill(matrixStack, new AABB(this.breakPos), this.startColor.getValue());
               }

               if (this.outline.getValue()) {
                  Render3DUtil.drawBox(matrixStack, new AABB(this.breakPos), this.startOutlineColor.getValue());
               }

               return;
            }

            if (this.box.getValue()) {
               Render3DUtil.drawFill(matrixStack, this.getFillBox(this.breakPos, easex), this.getColor(this.animationTime.ease(this.fadeEase.getValue())));
            }

            if (this.outline.getValue()) {
               Render3DUtil.drawBox(
                  matrixStack, this.getOutlineBox(this.breakPos, easex), this.getOutlineColor(this.animationTime.ease(this.fadeEase.getValue()))
               );
            }

            if (this.text.getValue()) {
               if (this.isAir(this.breakPos)) {
                  Render3DUtil.drawText3D("Waiting", this.breakPos.getCenter(), -1);
               } else if ((int)this.mineTimer.getMs() < this.breakFinalTime) {
                  Render3DUtil.drawText3D(this.df.format(progress * 100.0) + "%", this.breakPos.getCenter(), -1);
               } else {
                  Render3DUtil.drawText3D("100.0%", this.breakPos.getCenter(), -1);
               }
            }
         } else {
            progress = 0.0;
         }
      } else {
         progress = 0.0;
      }
   }

   private AABB getFillBox(BlockPos pos, double ease) {
      switch ((PacketMine.Animation)this.animation.getValue()) {
         case Center:
            ease = (1.0 - ease) / 2.0;
            return new AABB(pos).contract(ease, ease, ease).contract(-ease, -ease, -ease);
         case Grow:
            ease = (1.0 - ease) / 2.0;
            return new AABB(pos).contract(ease, 0.0, ease).contract(-ease, 0.0, -ease);
         case Up:
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1);
         case Down:
            return new AABB(pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
         case Oscillation:
            return new AABB(pos).contract(ease, ease, ease).contract(-ease, -ease, -ease);
         default:
            return new AABB(pos);
      }
   }

   private AABB getOutlineBox(BlockPos pos, double ease) {
      ease = Math.min(ease + this.expandLine.getValue(), 1.0);
      switch ((PacketMine.Animation)this.animation.getValue()) {
         case Center:
            ease = (1.0 - ease) / 2.0;
            return new AABB(pos).contract(ease, ease, ease).contract(-ease, -ease, -ease);
         case Grow:
            ease = (1.0 - ease) / 2.0;
            return new AABB(pos).contract(ease, 0.0, ease).contract(-ease, 0.0, -ease);
         case Up:
            return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + ease, pos.getZ() + 1);
         case Down:
            return new AABB(pos.getX(), pos.getY() + 1 - ease, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
         case Oscillation:
            return new AABB(pos).contract(ease, ease, ease).contract(-ease, -ease, -ease);
         default:
            return new AABB(pos);
      }
   }

   @EventListener(priority = -200)
   public void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck() && !mc.player.isCreative()) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            if (this.bypassGround.getValue()
               && !mc.player.isFallFlying()
               && this.breakPos != null
               && !this.isAir(this.breakPos)
               && this.bypassTime.getValue() > 0.0
               && Mth.sqrt((float)this.breakPos.getCenter().distanceToSqr(mc.player.getEyePosition())) <= this.range.getValueFloat() + 2.0F) {
               double breakTime = this.breakFinalTime - this.bypassTime.getValue();
               if (breakTime <= 0.0 || this.mineTimer.passed((long)breakTime)) {
                  this.sendGroundPacket = true;
                  ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(true);
               }
            } else {
               this.sendGroundPacket = false;
            }
         } else if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
            if (packet.getSlot() != this.lastSlot) {
               this.lastSlot = packet.getSlot();
               if (this.switchReset.getValue()) {
                  this.startPacket = false;
                  ghost = false;
                  complete = false;
                  this.mineTimer.reset();
                  this.animationTime.reset();
               }
            }
         } else if (event.getPacket() instanceof ServerboundPlayerActionPacket packetx) {
            if (packetx.getAction() == Action.START_DESTROY_BLOCK) {
               if (this.breakPos == null || !packetx.getPos().equals(this.breakPos)) {
                  return;
               }

               if (this.grimDisabler.getValue()) {
                  mc.getConnection().send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, packetx.getPos(), packetx.getDirection()));
               }

               this.startPacket = true;
            } else if (packetx.getAction() == Action.STOP_DESTROY_BLOCK) {
               if (this.breakPos == null || !packetx.getPos().equals(this.breakPos)) {
                  return;
               }

               if (!this.instant.getValue()) {
                  this.startPacket = false;
                  ghost = false;
                  complete = false;
               }
            }
         }
      }
   }

   boolean canPlaceCrystal(BlockPos pos) {
      BlockPos obsPos = pos.below();
      BlockPos boost = obsPos.above();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN)
         && BlockUtil.getClickSideStrict(obsPos) != null
         && this.noEntity(boost)
         && this.noEntity(boost.above())
         && (!ClientSetting.INSTANCE.lowVersion.getValue() || mc.level.isEmptyBlock(boost.above()));
   }

   boolean noEntity(BlockPos pos) {
      for (Entity entity : BlockUtil.getEntities(new AABB(pos))) {
         if (!(entity instanceof ItemEntity) && (!(entity instanceof ArmorStand) || !AntiCheat.INSTANCE.ignoreArmorStand.getValue())) {
            return false;
         }
      }

      return true;
   }

   void doSwap(int slot, int inv) {
      if (!this.inventory.getValue()) {
         InventoryUtil.switchToSlot(slot);
      } else {
         InventoryUtil.inventorySwap(inv, mc.player.getInventory().selected);
      }
   }

   int findCrystal() {
      return this.inventory.getValue() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
   }

   int findBlock(Block block) {
      return this.inventory.getValue() ? InventoryUtil.findBlockInventorySlot(block) : InventoryUtil.findBlock(block);
   }

   boolean shouldCrystal() {
      return this.crystal.getValue() && (!this.onlyHeadBomber.getValue() || this.obsidian.isPressed()) || CevBreaker.INSTANCE.isOn();
   }

   public static double getBreakTime(BlockPos pos) {
      int slot = INSTANCE.getTool(pos);
      if (slot == -1) {
         slot = mc.player.getInventory().selected;
      }

      return INSTANCE.getBreakTime(pos, slot);
   }

   double getBreakTime(BlockPos pos, int slot) {
      return this.getBreakTime(pos, slot, this.damage.getValue());
   }

   double getBreakTime(BlockPos pos, int slot, double damage) {
      return 1.0F / this.getBlockStrength(pos, mc.player.getInventory().getItem(slot)) / 20.0F * 1000.0F * damage;
   }

   float getBlockStrength(BlockPos position, ItemStack itemStack) {
      BlockState state = mc.level.getBlockState(position);
      float hardness = state.getDestroySpeed(mc.level, position);
      if (hardness < 0.0F) {
         return 0.0F;
      } else {
         float i = state.requiresCorrectToolForDrops() && !itemStack.isCorrectToolForDrops(state) ? 100.0F : 30.0F;
         return this.getDigSpeed(state, itemStack) / hardness / i;
      }
   }

   float getDigSpeed(BlockState state, ItemStack itemStack) {
      float digSpeed = this.getDestroySpeed(state, itemStack);
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

      if (mc.player.hasEffect(MobEffects.HASTE)) {
         digSpeed *= 1.0F + (mc.player.getEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2F;
      }

      if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
         digSpeed *= switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
      }

      if (mc.player.isUnderWater()) {
         digSpeed *= 1.0F;
      }

      boolean inWeb = this.checkWeb.getValue() && Alien.PLAYER.isInWeb(mc.player) && mc.level.getBlockState(this.breakPos).getBlock() == Blocks.COBWEB;
      if ((!mc.player.onGround() || inWeb)
         && INSTANCE.checkGround.getValue()
         && (!this.smart.getValue() || Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && Criticals.INSTANCE.isOn() || mc.player.isFallFlying() || inWeb)) {
         digSpeed /= 5.0F;
      }

      return digSpeed < 0.0F ? 0.0F : digSpeed;
   }

   float getDestroySpeed(BlockState state, ItemStack itemStack) {
      float destroySpeed = 1.0F;
      if (itemStack != null && !itemStack.isEmpty()) {
         destroySpeed *= itemStack.getDestroySpeed(state);
      }

      return destroySpeed;
   }

   Color getColor(double quad) {
      int sR = this.startColor.getValue().getRed();
      int sG = this.startColor.getValue().getGreen();
      int sB = this.startColor.getValue().getBlue();
      int sA = this.startColor.getValue().getAlpha();
      int eR = this.endColor.getValue().getRed();
      int eG = this.endColor.getValue().getGreen();
      int eB = this.endColor.getValue().getBlue();
      int eA = this.endColor.getValue().getAlpha();
      return new Color((int)(sR + (eR - sR) * quad), (int)(sG + (eG - sG) * quad), (int)(sB + (eB - sB) * quad), (int)(sA + (eA - sA) * quad));
   }

   Color getOutlineColor(double quad) {
      int sR = this.startOutlineColor.getValue().getRed();
      int sG = this.startOutlineColor.getValue().getGreen();
      int sB = this.startOutlineColor.getValue().getBlue();
      int sA = this.startOutlineColor.getValue().getAlpha();
      int eR = this.endOutlineColor.getValue().getRed();
      int eG = this.endOutlineColor.getValue().getGreen();
      int eB = this.endOutlineColor.getValue().getBlue();
      int eA = this.endOutlineColor.getValue().getAlpha();
      return new Color((int)(sR + (eR - sR) * quad), (int)(sG + (eG - sG) * quad), (int)(sB + (eB - sB) * quad), (int)(sA + (eA - sA) * quad));
   }

   int getTool(BlockPos pos) {
      if (this.hotBar.getValue()) {
         int index = -1;
         float CurrentFastest = 1.0F;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            float destroySpeed;
            float digSpeed;
            if (stack != ItemStack.EMPTY
               && (
                        digSpeed = EnchantmentHelper.getItemEnchantmentLevel(
                           mc.level.registryAccess().lookupOrThrow(Enchantments.EFFICIENCY.registryKey()).getOrThrow(Enchantments.EFFICIENCY), stack
                        )
                     )
                     + (destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos)))
                  > CurrentFastest) {
               CurrentFastest = digSpeed + destroySpeed;
               index = i;
            }
         }

         return index;
      } else {
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
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isGliding())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   boolean isAir(BlockPos breakPos) {
      return mc.level.isEmptyBlock(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
   }

   public static boolean unbreakable(BlockPos blockPos) {
      Block block = mc.level.getBlockState(blockPos).getBlock();
      return !(block instanceof AirBlock) && (block.defaultDestroyTime() == -1.0F || block.defaultDestroyTime() == 100.0F);
   }

   private static enum Animation {
      Center,
      Grow,
      Up,
      Down,
      Oscillation,
      None;
   }

   public static enum Page {
      General,
      Check,
      Place,
      Rotation,
      Render;
   }
}
