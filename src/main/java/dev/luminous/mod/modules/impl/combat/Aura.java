package dev.luminous.mod.modules.impl.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Aura extends Module {
   public static Aura INSTANCE;
   public static Entity target;
   public final EnumSetting<Aura.Page> page = this.add(new EnumSetting<>("Page", Aura.Page.General));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1F, 14.0, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Aura.Cooldown> cooldownMode = this.add(
      new EnumSetting<>("CooldownMode", Aura.Cooldown.Delay, () -> this.page.getValue() == Aura.Page.General)
   );
   private final BooleanSetting reset = this.add(
      new BooleanSetting("Reset", true, () -> this.page.getValue() == Aura.Page.General && this.cooldownMode.is(Aura.Cooldown.Delay))
   );
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting<>("Swing", SwingSide.All, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 1.1F, 0.0, 1.2F, 0.01, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting whileEating = this.add(new BooleanSetting("WhileUsing", true, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting weaponOnly = this.add(new BooleanSetting("WeaponOnly", true, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Timing> timing = this.add(new EnumSetting<>("Timing", Timing.All, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == Aura.Page.Target).setParent());
   private final BooleanSetting armorLow = this.add(
      new BooleanSetting("ArmorLow", true, () -> this.page.getValue() == Aura.Page.Target && this.Players.isOpen())
   );
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, () -> this.page.getValue() == Aura.Page.Target));
   private final EnumSetting<Aura.TargetMode> targetMode = this.add(
      new EnumSetting<>("Filter", Aura.TargetMode.DISTANCE, () -> this.page.getValue() == Aura.Page.Target)
   );
   private final EnumSetting<Aura.TargetESP> mode = this.add(
      new EnumSetting<>("TargetESP", Aura.TargetESP.Fill, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting<>("Ease", Easing.CubicInOut, () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting outlineColor = this.add(
      new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting hitOutlineColor = this.add(
      new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final Animation animation = new Animation();
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Aura.Page.Rotate));
   private final BooleanSetting yawStep = this.add(
      new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == Aura.Page.Rotate).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Aura.Page.Rotate)
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting fov = this.add(
      new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.getValue() && this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final Timer tick = new Timer();
   public Vec3 directionVec = null;

   public Aura() {
      super("Aura", Module.Category.Combat);
      this.setChinese("杀戮光环");
      INSTANCE = this;
   }

   public static void doRender(PoseStack stack, float partialTicks, Entity entity, Color color, Color outlineColor, Aura.TargetESP mode) {
      switch (mode) {
         case Fill: {
            AABB entityBox = ((IEntity)entity)
               .getDimensions()
               .makeBoundingBox(
                  new Vec3(
                     MathUtil.interpolate(entity.xOld, entity.getX(), (double)partialTicks),
                     MathUtil.interpolate(entity.yOld, entity.getY(), (double)partialTicks),
                     MathUtil.interpolate(entity.zOld, entity.getZ(), (double)partialTicks)
                  )
               )
               .inflate(0.0, 0.1, 0.0);
            Render3DScheduler.INSTANCE.addBox(entityBox, color, outlineColor, true, false);
            break;
         }
         case Box: {
            AABB entityBox = ((IEntity)entity)
               .getDimensions()
               .makeBoundingBox(
                  new Vec3(
                     MathUtil.interpolate(entity.xOld, entity.getX(), (double)partialTicks),
                     MathUtil.interpolate(entity.yOld, entity.getY(), (double)partialTicks),
                     MathUtil.interpolate(entity.zOld, entity.getZ(), (double)partialTicks)
                  )
               )
               .inflate(0.0, 0.1, 0.0);
            Render3DScheduler.INSTANCE.addBox(entityBox, color, outlineColor, true, true);
            break;
         }
         case Jello:
            JelloUtil.drawJello(stack, entity, color);
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, target, color);
      }
   }

   public static float getAttackCooldownProgressPerTick() {
      return (float)(1.0 / mc.player.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (target != null) {
         float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         this.doRender(matrixStack, partialTicks, target, this.mode.getValue());
      }
   }

   public void doRender(PoseStack stack, float partialTicks, Entity entity, Aura.TargetESP mode) {
      Color fillCol = ColorUtil.fadeColor(
         this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
      );
      Color outlineCol = ColorUtil.fadeColor(
         this.outlineColor.getValue(), this.hitOutlineColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), this.ease.getValue())
      );
      doRender(stack, partialTicks, entity, fillCol, outlineCol, mode);
   }

   @Override
   public String getInfo() {
      return target == null ? null : target.getName().getString();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (this.weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player)) {
               target = null;
            } else {
               target = this.getTarget(this.range.getValueFloat());
               if (target == null) {
                  target = this.getTarget(this.targetRange.getValueFloat());
               } else {
                  this.doAura();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (target != null && this.rotate.getValue() && this.shouldYawStep()) {
         this.directionVec = this.getAttackVec(target);
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (this.reset.getValue()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof ServerboundSwingPacket
            || packet instanceof ServerboundInteractPacket && Criticals.getInteractType((ServerboundInteractPacket)packet) == ActionType.ATTACK) {
            this.tick.reset();
         }
      }
   }

   private boolean check() {
      if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return false;
      } else {
         float cooldownProgress;
         if (this.cooldownMode.getValue() == Aura.Cooldown.Vanilla) {
            cooldownProgress = mc.player.getAttackStrengthScale(0.0F);
         } else {
            int at = (int)(this.tick.getMs() / 50L);
            at = (int)(at * Alien.SERVER.getTPSFactor());
            cooldownProgress = Math.max(at / getAttackCooldownProgressPerTick(), 0.0F);
         }

         if (this.cooldownMode.getValue() == Aura.Cooldown.Vanilla) {
            if (!(cooldownProgress >= 1.0F)) {
               return false;
            }
         } else if (!(cooldownProgress >= this.cooldown.getValue())) {
            return false;
         }

         return target instanceof LivingEntity entity && entity.hurtTime > this.hurtTime.getValue()
            ? false
            : this.whileEating.getValue() || !mc.player.isUsingItem();
      }
   }

   private void doAura() {
      if (this.check()) {
         if (this.rotate.getValue()) {
            Vec3 hitVec = this.getAttackVec(target);
            if (!this.faceVector(hitVec)) {
               return;
            }
         }

         this.animation.to = 1.0;
         this.animation.from = 1.0;
         mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
         mc.player.resetOnlyAttackStrengthTicker();
         EntityUtil.swingHand(InteractionHand.MAIN_HAND, this.swingMode.getValue());
         this.tick.reset();
         if (this.rotate.getValue() && !this.shouldYawStep()) {
            Alien.ROTATION.snapBack();
         }
      }
   }

   private Vec3 getAttackVec(Entity entity) {
      return MathUtil.getClosestPointToBox(mc.player.getEyePosition(), entity.getBoundingBox());
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || mc.player.getPose() != Pose.FALL_FLYING && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isGliding())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   public boolean faceVector(Vec3 directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) ? true : !this.checkFov.getValue();
      }
   }

   public Entity getTarget(double range) {
      Entity target = null;
      double distance = range;
      double maxHealth = 36.0;

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (this.isEnemy(entity)) {
            Vec3 hitVec = this.getAttackVec(entity);
            if (!(mc.player.getEyePosition().distanceTo(hitVec) > range)
               && (mc.player.hasLineOfSight(entity) || !(mc.player.getEyePosition().distanceTo(hitVec) > this.wallRange.getValue()))
               && CombatUtil.isValid(entity)) {
               if (target == null) {
                  target = entity;
                  distance = mc.player.getEyePosition().distanceTo(hitVec);
                  maxHealth = EntityUtil.getHealth(entity);
               } else {
                  if (this.armorLow.getValue() && entity instanceof Player && EntityUtil.isArmorLow((Player)entity, 10)) {
                     target = entity;
                     break;
                  }

                  if (this.targetMode.getValue() == Aura.TargetMode.HEALTH && EntityUtil.getHealth(entity) < maxHealth) {
                     target = entity;
                     maxHealth = EntityUtil.getHealth(entity);
                  } else if (this.targetMode.getValue() == Aura.TargetMode.DISTANCE && mc.player.getEyePosition().distanceTo(hitVec) < distance) {
                     target = entity;
                     distance = mc.player.getEyePosition().distanceTo(hitVec);
                  }
               }
            }
         }
      }

      return target;
   }

   private boolean isEnemy(Entity entity) {
      if (entity instanceof Slime) {
         return this.Slimes.getValue();
      } else if (entity instanceof Player) {
         return this.Players.getValue();
      } else if (entity instanceof Villager || entity instanceof WanderingTrader) {
         return this.Villagers.getValue();
      } else if (entity instanceof Animal) {
         return this.Animals.getValue();
      } else {
         return entity instanceof Mob ? this.Mobs.getValue() : false;
      }
   }

   public static enum Cooldown {
      Vanilla,
      Delay;
   }

   public static enum Mode {
      Mace,
      Axe,
      Sword;
   }

   public static enum Page {
      General,
      Rotate,
      Target,
      Render;
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack,
      None;
   }

   private static enum TargetMode {
      DISTANCE,
      HEALTH;
   }
}
