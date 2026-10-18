package dev.luminous.api.utils.entity;

import com.mojang.authlib.GameProfile;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.PredictUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public class CopyPlayerEntity extends Player {
   private final boolean onGround;

   public CopyPlayerEntity(Player player) {
      this(player, false, 0.0, 0, 0, false, false, false, false);
   }

   public CopyPlayerEntity(Player player, boolean effect) {
      super(Wrapper.mc.level, new GameProfile(player.getGameProfile().id(), player.getGameProfile().name()));
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
      this.setDeltaMovement(player.getDeltaMovement());
      this.getInventory().replaceWith(player.getInventory());
      if (effect) {
         for (MobEffectInstance se : new ArrayList<>(player.getActiveEffects())) {
            this.addEffect(se);
         }
      }

      this.internalSetAbsorptionAmount(player.getAbsorptionAmount());
      this.setHealth(player.getHealth());
      this.setBoundingBox(player.getBoundingBox());
   }

   public CopyPlayerEntity(
      Player player, boolean effect, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause
   ) {
      this(player, effect);
      if (ticks > 0) {
         this.setPos(PredictUtil.getPos(player, maxMotionY, ticks, simulation, step, doubleStep, jump, inBlockPause));
      }
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

   @Override
   public GameType gameMode() {
      return GameType.SURVIVAL;
   }
}
