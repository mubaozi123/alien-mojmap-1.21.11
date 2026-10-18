package dev.luminous.mod.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.DamageUtils;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.ActionType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class FakePlayer extends Module {
   public static FakePlayer INSTANCE;
   public static FakePlayer.FakePlayerEntity fakePlayer;
   final StringSetting name = this.add(new StringSetting("Name", "FakePlayer"));
   private final BooleanSetting damage = this.add(new BooleanSetting("Damage", true));
   private final BooleanSetting autoTotem = this.add(new BooleanSetting("AutoTotem", true));
   public final BooleanSetting record = this.add(new BooleanSetting("Record", false));
   public final BooleanSetting play = this.add(new BooleanSetting("Play", false));
   final List<FakePlayer.PlayerState> positions = new ArrayList<>();
   int movementTick;
   boolean lastRecordValue = false;

   public FakePlayer() {
      super("FakePlayer", Module.Category.Misc);
      this.setChinese("假人");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.name.getValue();
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         fakePlayer = new FakePlayer.FakePlayerEntity(mc.player, this.name.getValue());
         mc.level.addEntity(fakePlayer);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (fakePlayer != null && fakePlayer.level() == mc.level) {
         if (this.autoTotem.getValue()) {
            if (fakePlayer.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (fakePlayer.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
         }

         if (this.record.getValue() != this.lastRecordValue && this.record.getValue()) {
            this.positions.clear();
         }

         this.lastRecordValue = this.record.getValue();
         if (this.record.getValue()) {
            this.positions.add(new FakePlayer.PlayerState(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()));
         }

         if (this.play.getValue() && !this.positions.isEmpty()) {
            this.movementTick++;
            if (this.movementTick >= this.positions.size()) {
               this.movementTick = 0;
            }

            FakePlayer.PlayerState p = this.positions.get(this.movementTick);
            fakePlayer.setYRot(p.yaw);
            fakePlayer.setXRot(p.pitch);
            fakePlayer.setYHeadRot(p.yaw);
            fakePlayer.syncPacketPositionCodec(p.x, p.y, p.z);
            fakePlayer.moveOrInterpolateTo(new Vec3(p.x, p.y, p.z), p.yaw, p.pitch);
         }
      } else {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      if (fakePlayer != null) {
         fakePlayer.discard();
         fakePlayer.setRemoved(RemovalReason.KILLED);
         fakePlayer.onClientRemoval();
         fakePlayer = null;
      }
   }

   @EventListener
   public void onAttack(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundInteractPacket packet
         && Criticals.getInteractType(packet) == ActionType.ATTACK
         && Criticals.getEntity(packet) == fakePlayer) {
         mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
         float damage = DamageUtils.getAttackDamage(mc.player, fakePlayer);
         if ((
               mc.player.fallDistance > 0.0
                  || Criticals.INSTANCE.isOn()
                     && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground)
                     && (mc.player.onGround() || !Criticals.INSTANCE.onlyGround.getValue())
            )
            && (!mc.player.onGround() || Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground))
            && !mc.player.onClimbable()
            && !mc.player.isInWater()
            && !mc.player.hasEffect(MobEffects.BLINDNESS)
            && !mc.player.isPassenger()) {
            mc.level
               .playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            mc.player.crit(fakePlayer);
         }

         if (fakePlayer.hurtTime <= 0) {
            fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
            if (fakePlayer.getAbsorptionAmount() >= damage) {
               fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
            } else {
               float damage2 = damage - fakePlayer.getAbsorptionAmount();
               fakePlayer.setAbsorptionAmount(0.0F);
               fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
            }

            if (fakePlayer.isDeadOrDying()) {
               Alien.POP.onTotemPop(fakePlayer);
               if (fakePlayer.checkTotemDeathProtection(mc.level.damageSources().generic())) {
                  fakePlayer.setHealth(10.0F);
                  new ClientboundEntityEventPacket(fakePlayer, (byte)35).handle((ClientGamePacketListener)mc.getConnection());
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      Packet<?> packet;
      if (this.damage.getValue() && fakePlayer != null && fakePlayer.hurtTime <= 0 && (packet = event.getPacket()) instanceof ClientboundExplodePacket) {
         ClientboundExplodePacket explosion = (ClientboundExplodePacket)packet;
         Vec3 vec3d = explosion.center();
         if (Math.sqrt(vec3d.distanceToSqr(fakePlayer.position())) > 10.0) {
            return;
         }

         float damage = BlockUtil.getBlock(new BlockPosX(explosion.center())) == Blocks.RESPAWN_ANCHOR
            ? (float)AutoAnchor.INSTANCE.getAnchorDamage(new BlockPosX(explosion.center()), fakePlayer, fakePlayer)
            : AutoCrystal.INSTANCE.calculateDamage(explosion.center(), fakePlayer, fakePlayer);
         fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
         if (fakePlayer.getAbsorptionAmount() >= damage) {
            fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
         } else {
            float damage2 = damage - fakePlayer.getAbsorptionAmount();
            fakePlayer.setAbsorptionAmount(0.0F);
            fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
         }

         if (fakePlayer.isDeadOrDying()) {
            Alien.POP.onTotemPop(fakePlayer);
            if (fakePlayer.checkTotemDeathProtection(mc.level.damageSources().generic())) {
               fakePlayer.setHealth(10.0F);
               new ClientboundEntityEventPacket(fakePlayer, (byte)35).handle((ClientGamePacketListener)mc.getConnection());
            }
         }
      }
   }

   public static class FakePlayerEntity extends RemotePlayer {
      private final boolean onGround;

      public FakePlayerEntity(Player player, String name) {
         super(Wrapper.mc.level, new GameProfile(UUID.fromString("66666666-6666-6666-6666-666666666666"), name));
         this.copyPosition(player);
         this.xOld = player.xOld;
         this.zOld = player.zOld;
         this.yOld = player.yOld;
         this.yBodyRot = player.yBodyRot;
         this.yHeadRot = player.yHeadRot;
         this.attackAnim = player.attackAnim;
         this.swingTime = player.swingTime;
         this.walkAnimation.setSpeed(player.walkAnimation.speed());
         this.walkAnimation.position = player.walkAnimation.position();
         ((ILivingEntity)this).setLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         ((ILivingEntity)this).setLastLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         this.wasTouchingWater = player.isInWater();
         this.setShiftKeyDown(player.isShiftKeyDown());
         this.setPose(player.getPose());
         this.setSharedFlag(7, player.isFallFlying());
         this.onGround = player.onGround();
         this.setOnGround(this.onGround);
         this.getInventory().replaceWith(player.getInventory());
         this.internalSetAbsorptionAmount(player.getAbsorptionAmount());
         this.setHealth(player.getHealth());
         this.setBoundingBox(player.getBoundingBox());
      }

      @Override
      public boolean onGround() {
         return this.onGround;
      }

      @Override
      public boolean isSpectator() {
         return false;
      }

      @Override
      public boolean isCreative() {
         return false;
      }
   }

   private record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
