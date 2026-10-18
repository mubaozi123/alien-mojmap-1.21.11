package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IPlayerInteractEntityC2SPacket;
import dev.luminous.asm.accessors.IPlayerMoveC2SPacket;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.impl.exploit.Phase;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;

public class Criticals extends Module {
   public static Criticals INSTANCE;
   public final EnumSetting<Criticals.Mode> mode = this.add(new EnumSetting<>("Mode", Criticals.Mode.OldNCP));
   public final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true, () -> !this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting setOnGround = this.add(new BooleanSetting("SetNoGround", false, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting blockCheck = this.add(new BooleanSetting("BlockCheck", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(Criticals.Mode.Ground)).setParent());
   private final BooleanSetting mini = this.add(new BooleanSetting("Mini", true, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen()));
   private final SliderSetting y = this.add(
      new SliderSetting("MotionY", 0.05, 0.0, 1.0, 1.0E-10, () -> this.mode.is(Criticals.Mode.Ground) && this.autoJump.isOpen())
   );
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting crawlingDisable = this.add(new BooleanSetting("CrawlingDisable", true, () -> this.mode.is(Criticals.Mode.Ground)));
   private final BooleanSetting flight = this.add(new BooleanSetting("Flight", false, () -> this.mode.is(Criticals.Mode.Ground)));
   boolean requireJump = false;

   public Criticals() {
      super("Criticals", Module.Category.Combat);
      this.setChinese("刀刀暴击");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.mode.getValue().name();
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled()) {
         if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
            if (this.mode.is(Criticals.Mode.Ground)) {
               if (!BowBomb.send) {
                  if (!AutoPearl.throwing && !Phase.INSTANCE.isOn()) {
                     if (this.setOnGround.getValue()) {
                        if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
                           ((IPlayerMoveC2SPacket)event.getPacket()).setOnGround(false);
                        }
                     }
                  }
               }
            } else {
               Entity entity;
               if (event.getPacket() instanceof ServerboundInteractPacket packet
                  && getInteractType(packet) == ActionType.ATTACK
                  && !((entity = getEntity(packet)) instanceof EndCrystal)
                  && (!this.onlyGround.getValue() || mc.player.onGround() || mc.player.getAbilities().flying)
                  && !mc.player.isInLava()
                  && !mc.player.isInWater()
                  && entity != null) {
                  this.doCrit(entity);
               }
            }
         }
      }
   }

   @Override
   public void onLogout() {
      if (this.mode.is(Criticals.Mode.Ground) && this.autoDisable.getValue()) {
         this.disable();
      }
   }

   @Override
   public void onEnable() {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         this.requireJump = true;
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (nullCheck()) {
               if (this.autoDisable.getValue()) {
                  this.disable();
               }
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.crawlingDisable.getValue() && mc.player.isVisuallyCrawling()) {
               this.disable();
            } else if (mc.player.onGround()
               && this.autoJump.getValue()
               && (!this.blockCheck.getValue() || BlockUtil.canCollide(mc.player, new AABB(EntityUtil.getPlayerPos(true).above(2))))) {
               this.jump();
            }
         }
      }
   }

   public void jump() {
      if (this.mini.getValue()) {
         MovementUtil.setMotionY(this.y.getValue());
      } else {
         mc.player.jumpFromGround();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         if (this.mode.is(Criticals.Mode.Ground)) {
            if (this.crawlingDisable.getValue() && mc.player.isVisuallyCrawling()) {
               this.disable();
            } else if (MovementUtil.isMoving() && this.autoDisable.getValue()) {
               this.disable();
            } else if (this.flight.getValue() && mc.player.fallDistance > 0.0) {
               MovementUtil.setMotionY(0.0);
               MovementUtil.setMotionX(0.0);
               MovementUtil.setMotionZ(0.0);
               this.requireJump = false;
            } else if (this.blockCheck.getValue() && !BlockUtil.canCollide(mc.player, new AABB(EntityUtil.getPlayerPos(true).above(2)))) {
               this.requireJump = true;
            } else if (mc.player.onGround() && this.autoJump.getValue() && (this.flight.getValue() || this.requireJump)) {
               this.jump();
               this.requireJump = false;
            }
         }
      }
   }

   public void doCrit(Entity entity) {
      switch ((Criticals.Mode)this.mode.getValue()) {
         case UpdatedNCP:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
            break;
         case Strict:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
            break;
         case NCP:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
            break;
         case OldNCP:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.058293536E-5, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 9.16580235E-6, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.0371854E-7, mc.player.getZ(), false, false));
            break;
         case Hypixel2K22:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.0045, mc.player.getZ(), true, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.52121E-4, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.025, mc.player.getZ(), false, false));
            break;
         case Packet:
            mc.player.crit(entity);
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 5.0E-4, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 1.0E-4, mc.player.getZ(), false, false));
         case Ground:
         default:
            break;
         case BBTT:
            if (MovementUtil.isMoving() || !MovementUtil.isStatic()) {
               return;
            }

            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false));
            mc.getConnection().send(new Pos(mc.player.getX(), mc.player.getY() + 0.045, mc.player.getZ(), false, false));
      }
   }

   public static Entity getEntity(ServerboundInteractPacket packet) {
      return mc.level == null ? null : mc.level.getEntity(((IPlayerInteractEntityC2SPacket)packet).getEntityId());
   }

   public static ActionType getInteractType(ServerboundInteractPacket packet) {
      return packet.action.getType();
   }

   public static enum Mode {
      UpdatedNCP,
      Strict,
      NCP,
      OldNCP,
      Hypixel2K22,
      Packet,
      Ground,
      BBTT;
   }
}
