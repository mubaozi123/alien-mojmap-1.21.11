package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TravelEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.asm.accessors.IFireworkRocketEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends Module {
   public static ElytraFly INSTANCE;
   public final EnumSetting<ElytraFly.Mode> mode = this.add(new EnumSetting<>("Mode", ElytraFly.Mode.Control));
   public final BooleanSetting infiniteDura = this.add(new BooleanSetting("InfiniteDura", false));
   public final BooleanSetting packet = this.add(new BooleanSetting("Packet", false).setParent());
   private final SliderSetting packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, this.packet::isOpen));
   private final BooleanSetting setFlag = this.add(new BooleanSetting("SetFlag", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting firework = this.add(new BooleanSetting("Firework", false).setParent());
   public final BindSetting fireWork = this.add(new BindSetting("FireWorkBind", -1, this.firework::isOpen));
   public final BooleanSetting packetInteract = this.add(new BooleanSetting("PacketInteract", true, this.firework::isOpen));
   public final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, this.firework::isOpen));
   public final BooleanSetting onlyOne = this.add(new BooleanSetting("OnlyOne", true, this.firework::isOpen));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, this.firework::isOpen));
   public final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   public final SliderSetting upPitch = this.add(new SliderSetting("UpPitch", 0.0, 0.0, 90.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting upFactor = this.add(new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting downFactor = this.add(new SliderSetting("FallSpeed", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.1F, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final BooleanSetting speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting maxSpeed = this.add(
      new SliderSetting("MaxSpeed", 2.5, 0.1F, 10.0, () -> this.speedLimit.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
   );
   public final BooleanSetting noDrag = this.add(new BooleanSetting("NoDrag", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final Timer fireworkTimer = new Timer();
   private final BooleanSetting autoStop = this.add(new BooleanSetting("AutoStop", true));
   private final BooleanSetting sprint = this.add(new BooleanSetting("Sprint", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting pitch = this.add(new SliderSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting instantFly = this.add(new BooleanSetting("AutoStart", true, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting checkSpeed = this.add(new BooleanSetting("CheckSpeed", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   public final SliderSetting minSpeed = this.add(new SliderSetting("MinSpeed", 70.0, 0.1, 200.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 1000.0, 0.0, 20000.0, 50.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting timeout = this.add(new SliderSetting("Timeout", 0.0, 0.1, 1.0, 0.1, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   private final SliderSetting boost = this.add(new SliderSetting("Boost", 1.0, 0.1, 4.0, () -> this.mode.getValue() == ElytraFly.Mode.Boost));
   private final BooleanSetting freeze = this.add(new BooleanSetting("Freeze", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
   private final BooleanSetting motionStop = this.add(new BooleanSetting("MotionStop", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
   private final SliderSetting infiniteMaxSpeed = this.add(
      new SliderSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   private final SliderSetting infiniteMinSpeed = this.add(
      new SliderSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   private final SliderSetting infiniteMaxHeight = this.add(
      new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   public final BooleanSetting releaseSneak = this.add(new BooleanSetting("ReleaseSneak", false));
   private final Timer instantFlyTimer = new Timer();
   boolean prev;
   float prePitch;
   private boolean hasElytra = false;
   float yaw = 0.0F;
   float rotationPitch = 0.0F;
   boolean flying = false;
   int packetDelayInt = 0;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;

   public ElytraFly() {
      super("ElytraFly", Module.Category.Movement);
      this.setChinese("鞘翅飞行");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ElytraFly.FireWorkTweak());
   }

   public void off() {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.onlyOne.getValue()) {
            for (Entity entity : Alien.THREAD.getEntities()) {
               if (entity instanceof FireworkRocketEntity fireworkRocketEntity && ((IFireworkRocketEntity)fireworkRocketEntity).getShooter() == mc.player) {
                  return;
               }
            }
         }

         INSTANCE.fireworkTimer.reset();
         if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            if (this.packetInteract.getValue()) {
               sendSequencedPacket(
                  id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
               );
            } else {
               mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
         } else {
            int firework;
            if (this.inventory.getValue() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selected);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
               }

               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selected);
               EntityUtil.syncInventory();
            } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
               int old = mc.player.getInventory().selected;
               InventoryUtil.switchToSlot(firework);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(
                     id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
               }

               InventoryUtil.switchToSlot(old);
            }
         }
      }
   }

   public static boolean recastElytra(LocalPlayer player) {
      if (checkConditions(player) && ignoreGround(player)) {
         player.connection.send(new ServerboundPlayerCommandPacket(player, Action.START_FALL_FLYING));
         if (INSTANCE.setFlag.getValue()) {
            mc.player.startFallFlying();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(LocalPlayer player) {
      ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
      return !player.getAbilities().flying
         && !player.isPassenger()
         && !player.onClimbable()
         && itemStack.is(Items.ELYTRA)
         && itemStack.getDamageValue() < itemStack.getMaxDamage() - 1;
   }

   private static boolean ignoreGround(LocalPlayer player) {
      if (!player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
         ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
         if (itemStack.is(Items.ELYTRA) && itemStack.getDamageValue() < itemStack.getMaxDamage() - 1) {
            player.startFallFlying();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.hasElytra = false;
         this.yaw = mc.player.getYRot();
         this.rotationPitch = mc.player.getXRot();
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         if (this.releaseSneak.getValue()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SLEEPING));
         }
      }
   }

   private void boost() {
      if (this.hasElytra) {
         if (!this.isGliding()) {
            return;
         }

         float yaw = (float)Math.toRadians(mc.player.getYRot());
         if (mc.options.keyUp.isDown()) {
            mc.player.push(-Mth.sin(yaw) * this.boost.getValueFloat() / 10.0F, 0.0, Mth.cos(yaw) * this.boost.getValueFloat() / 10.0F);
         }
      }
   }

   @EventListener(priority = -9999)
   public void onRotation(UpdateRotateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Rotation)) {
            if (this.isGliding()) {
               if (MovementUtil.isMoving()) {
                  if (mc.options.keyJump.isDown()) {
                     this.rotationPitch = -45.0F;
                  } else if (mc.options.keyShift.isDown()) {
                     this.rotationPitch = 45.0F;
                  } else {
                     this.rotationPitch = -1.9F;
                     if (this.motionStop.getValue()) {
                        this.setY(0.0);
                     }
                  }
               } else if (mc.options.keyJump.isDown()) {
                  this.rotationPitch = -89.0F;
               } else if (mc.options.keyShift.isDown()) {
                  this.rotationPitch = 89.0F;
               } else if (this.motionStop.getValue()) {
                  this.setY(0.0);
               }

               if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
                  this.yaw = Sprint.getSprintYaw(mc.player.getYRot());
               } else if (this.motionStop.getValue()) {
                  this.setX(0.0);
                  this.setZ(0.0);
               }

               event.setYaw(this.yaw);
               event.setPitch(this.rotationPitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Pitch)) {
            if (this.isGliding()) {
               event.setPitch(this.infinitePitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Bounce) && this.isGliding()) {
            event.setPitch(this.pitch.getValueFloat());
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.getInfinitePitch();
      this.flying = false;
      if (this.packet.getValue()) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      } else {
         this.hasElytra = false;

         for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack is = mc.player.getItemBySlot(slot);
            if (is.is(Items.ELYTRA)) {
               this.hasElytra = true;
               break;
            }
         }

         if (this.infiniteDura.getValue() && !mc.player.onGround() && this.hasElytra) {
            this.flying = true;
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, 0, ClickType.PICKUP, mc.player);
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            if (this.setFlag.getValue()) {
               mc.player.startFallFlying();
            }
         }

         if (this.mode.is(ElytraFly.Mode.Bounce)) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(0);
            return;
         }
      }

      double x = mc.player.getX() - mc.player.xo;
      double y = mc.player.getY() - mc.player.yo;
      double z = mc.player.getZ() - mc.player.zo;
      double dist = Math.sqrt(x * x + z * z + y * y) / 1000.0;
      double div = 1.388888888888889E-5;
      float timer = Alien.TIMER.get();
      double speed = dist / div * timer;
      if (this.mode.getValue() == ElytraFly.Mode.Boost) {
         this.boost();
      }

      if (this.packet.getValue()) {
         if (!mc.player.onGround()) {
            this.packetDelayInt++;
            if (!(this.packetDelayInt <= this.packetDelay.getValue())) {
               int elytra = InventoryUtil.findItem(Items.ELYTRA);
               if (elytra != -1) {
                  mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, elytra, ClickType.SWAP, mc.player);
                  mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
                  mc.player.startFallFlying();
                  if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
                     && this.firework.getValue()
                     && this.fireworkTimer.passed(this.delay.getValueInt())
                     && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.keyJump.isDown())
                     && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                     && this.isGliding()) {
                     this.off();
                     this.fireworkTimer.reset();
                  }

                  mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, elytra, ClickType.SWAP, mc.player);
                  this.packetDelayInt = 0;
               } else {
                  elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                  if (elytra != -1) {
                     mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, elytra, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, 0, ClickType.PICKUP, mc.player);
                     mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
                        && this.firework.getValue()
                        && this.fireworkTimer.passed(this.delay.getValueInt())
                        && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.keyJump.isDown())
                        && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                        && this.isGliding()) {
                        this.off();
                        this.fireworkTimer.reset();
                     }

                     mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 6, 0, ClickType.PICKUP, mc.player);
                     mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, elytra, 0, ClickType.PICKUP, mc.player);
                     this.packetDelayInt = 0;
                  }
               }
            }
         }
      } else {
         if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
            && this.firework.getValue()
            && this.fireworkTimer.passed(this.delay.getValueInt())
            && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.keyJump.isDown())
            && (!mc.player.isUsingItem() || !this.usingPause.getValue())
            && this.isGliding()) {
            this.off();
            this.fireworkTimer.reset();
         }

         if (!this.isGliding() && this.hasElytra) {
            this.fireworkTimer.setMs(99999999L);
            if (!mc.player.onGround() && this.instantFly.getValue() && mc.player.getDeltaMovement().y() < 0.0 && !this.infiniteDura.getValue()) {
               if (!this.instantFlyTimer.passed((long)(1000.0 * this.timeout.getValue()))) {
                  return;
               }

               this.instantFlyTimer.reset();
               mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
               if (this.setFlag.getValue()) {
                  mc.player.startFallFlying();
               }
            }
         }
      }
   }

   protected final Vec3 getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   public Vec3 getRotationVec(float tickDelta) {
      return this.getRotationVector(-this.upPitch.getValueFloat(), mc.player.getViewYRot(tickDelta));
   }

   @EventListener
   private void onPlayerMove(MoveEvent event) {
      if (this.autoStop.getValue() && this.isGliding()) {
         int chunkX = (int)(mc.player.getX() / 16.0);
         int chunkZ = (int)(mc.player.getZ() / 16.0);
         if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            event.cancel();
         }
      }
   }

   @EventListener
   private void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
            if (this.autoJump.getValue()) {
               mc.options.keyJump.setDown(true);
            }

            if (event.isPost()) {
               if (!this.isGliding()) {
                  mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
               }

               if (checkConditions(mc.player) && !this.sprint.getValue()) {
                  if (this.isGliding()) {
                     mc.player.setSprinting(mc.player.onGround());
                  } else {
                     mc.player.setSprinting(true);
                  }
               }
            } else if (checkConditions(mc.player) && this.sprint.getValue()) {
               mc.player.setSprinting(true);
            }
         }
      }
   }

   @EventListener
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce)
            && this.hasElytra
            && event.getPacket() instanceof ServerboundPlayerCommandPacket
            && ((ServerboundPlayerCommandPacket)event.getPacket()).getAction().equals(Action.START_FALL_FLYING)
            && !this.sprint.getValue()) {
            mc.player.setSprinting(true);
         }
      }
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            mc.player.stopFallFlying();
         }
      }
   }

   @EventListener
   public void travel(TravelEvent event) {
      if (!nullCheck()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getXRot();
                  mc.player.setXRot(this.pitch.getValueFloat());
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setXRot(this.prePitch);
               }
            } else if (this.mode.is(ElytraFly.Mode.Pitch) && this.isGliding()) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getXRot();
                  mc.player.setXRot(this.lastInfinitePitch);
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setXRot(this.prePitch);
               }
            }
         }
      }
   }

   @EventListener
   public void onMove(TravelEvent event) {
      if (!nullCheck() && this.hasElytra && this.isGliding() && !event.isPost()) {
         if ((this.mode.is(ElytraFly.Mode.Freeze) || this.mode.is(ElytraFly.Mode.Rotation) && this.freeze.getValue())
            && !MovementUtil.isMoving()
            && !mc.options.keyJump.isDown()
            && !mc.options.keyShift.isDown()) {
            event.cancel();
         } else {
            if (this.mode.getValue() == ElytraFly.Mode.Control) {
               if (this.firework.getValue()) {
                  if (mc.options.keyShift.isDown() && mc.options.keyJump.isDown()) {
                     this.setY(0.0);
                  } else if (mc.options.keyShift.isDown()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (mc.options.keyJump.isDown()) {
                     this.setY(this.upFactor.getValue());
                  } else {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                  this.setX(dir[0]);
                  this.setZ(dir[1]);
               } else {
                  Vec3 lookVec = this.getRotationVec(mc.getDeltaTracker().getGameTimeDeltaTicks());
                  double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                  double motionDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (mc.options.keyShift.isDown()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (!mc.options.keyJump.isDown()) {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  if (mc.options.keyJump.isDown()) {
                     if (motionDist > this.upFactor.getValue() / this.upFactor.getMax()) {
                        double rawUpSpeed = motionDist * 0.01325;
                        this.setY(this.getY() + rawUpSpeed * 3.2);
                        this.setX(this.getX() - lookVec.x * rawUpSpeed / lookDist);
                        this.setZ(this.getZ() - lookVec.z * rawUpSpeed / lookDist);
                     } else {
                        double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                        this.setX(dir[0]);
                        this.setZ(dir[1]);
                     }
                  }

                  if (lookDist > 0.0) {
                     this.setX(this.getX() + (lookVec.x / lookDist * motionDist - this.getX()) * 0.1);
                     this.setZ(this.getZ() + (lookVec.z / lookDist * motionDist - this.getZ()) * 0.1);
                  }

                  if (!mc.options.keyJump.isDown()) {
                     double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                     this.setX(dir[0]);
                     this.setZ(dir[1]);
                  }

                  if (!this.noDrag.getValue()) {
                     this.setY(this.getY() * 0.99F);
                     this.setX(this.getX() * 0.98F);
                     this.setZ(this.getZ() * 0.99F);
                  }

                  double finalDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (this.speedLimit.getValue() && finalDist > this.maxSpeed.getValue()) {
                     this.setX(this.getX() * this.maxSpeed.getValue() / finalDist);
                     this.setZ(this.getZ() * this.maxSpeed.getValue() / finalDist);
                  }

                  event.cancel();
                  mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
               }
            }
         }
      }
   }

   private double getX() {
      return MovementUtil.getMotionX();
   }

   private void setX(double f) {
      MovementUtil.setMotionX(f);
   }

   private double getY() {
      return MovementUtil.getMotionY();
   }

   private void setY(double f) {
      MovementUtil.setMotionY(f);
   }

   private double getZ() {
      return MovementUtil.getMotionZ();
   }

   private void setZ(double f) {
      MovementUtil.setMotionZ(f);
   }

   private void getInfinitePitch() {
      this.lastInfinitePitch = this.infinitePitch;
      double currentPlayerSpeed = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo);
      if (mc.player.getY() < this.infiniteMaxHeight.getValue()) {
         if (currentPlayerSpeed * 72.0 < this.infiniteMinSpeed.getValue() && !this.down) {
            this.down = true;
         }

         if (currentPlayerSpeed * 72.0 > this.infiniteMaxSpeed.getValue() && this.down) {
            this.down = false;
         }
      } else {
         this.down = true;
      }

      if (this.down) {
         this.infinitePitch += 3.0F;
      } else {
         this.infinitePitch -= 3.0F;
      }

      this.infinitePitch = MathUtil.clamp(this.infinitePitch, -40.0F, 40.0F);
   }

   public boolean isGliding() {
      return mc.player.getPose() == Pose.FALL_FLYING || this.packet.getValue() && this.hasElytra && !mc.player.onGround() || this.flying;
   }

   public class FireWorkTweak {
      boolean press;

      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!ElytraFly.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.screen == null) {
                  if (ElytraFly.this.fireWork.isPressed()) {
                     if (!this.press
                        && ElytraFly.this.fireworkTimer.passed(ElytraFly.this.delay.getValueInt())
                        && (!Wrapper.mc.player.isUsingItem() || !ElytraFly.this.usingPause.getValue())
                        && ElytraFly.this.isGliding()) {
                        ElytraFly.this.off();
                        ElytraFly.this.fireworkTimer.reset();
                     }

                     this.press = true;
                  } else {
                     this.press = false;
                  }
               } else {
                  this.press = false;
               }
            }
         }
      }
   }

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch;
   }
}
