package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PlaySoundEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import java.util.ArrayList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class NoSound extends Module {
   static final ArrayList<SoundEvent> armor = new ArrayList<>();
   public static NoSound INSTANCE;
   private final BooleanSetting equip = this.add(new BooleanSetting("ArmorEquip", true));
   private final BooleanSetting explode = this.add(new BooleanSetting("Explode", true));
   private final BooleanSetting attack = this.add(new BooleanSetting("Attack", true));
   private final BooleanSetting teleport = this.add(new BooleanSetting("Teleport", true));
   private final BooleanSetting throwConfig = this.add(new BooleanSetting("Throw", true));
   private final BooleanSetting potion = this.add(new BooleanSetting("Potion", true));
   private final BooleanSetting elytra = this.add(new BooleanSetting("Elytra", true));

   public NoSound() {
      super("NoSound", Module.Category.Misc);
      this.setChinese("去除声音");
      INSTANCE = this;
   }

   @EventListener
   public void onPlaySound(PlaySoundEvent event) {
      if (this.equip.getValue()) {
         for (SoundEvent se : armor) {
            if (event.sound.getIdentifier() == se.location()) {
               event.cancel();
               return;
            }
         }
      }

      if (!this.explode.getValue()
         || event.sound.getIdentifier() != SoundEvents.GENERIC_EXPLODE.value().location()
            && event.sound.getIdentifier() != SoundEvents.DRAGON_FIREBALL_EXPLODE.location()) {
         if (this.attack.getValue()
            && (
               event.sound.getIdentifier() == SoundEvents.PLAYER_ATTACK_WEAK.location()
                  || event.sound.getIdentifier() == SoundEvents.PLAYER_ATTACK_KNOCKBACK.location()
                  || event.sound.getIdentifier() == SoundEvents.PLAYER_ATTACK_STRONG.location()
            )) {
            event.cancel();
         }

         if (this.teleport.getValue() && event.sound.getIdentifier() == SoundEvents.PLAYER_TELEPORT.location()) {
            event.cancel();
         }

         if (this.potion.getValue() && event.sound.getIdentifier() == SoundEvents.SPLASH_POTION_BREAK.location()) {
            event.cancel();
         }

         if (this.elytra.getValue() && event.sound.getIdentifier() == SoundEvents.ELYTRA_FLYING.location()) {
            event.cancel();
         }

         if (this.throwConfig.getValue()
            && (
               event.sound.getIdentifier() == SoundEvents.ENDER_PEARL_THROW.location()
                  || event.sound.getIdentifier() == SoundEvents.EGG_THROW.location()
                  || event.sound.getIdentifier() == SoundEvents.EXPERIENCE_BOTTLE_THROW.location()
                  || event.sound.getIdentifier() == SoundEvents.SNOWBALL_THROW.location()
                  || event.sound.getIdentifier() == SoundEvents.SPLASH_POTION_THROW.location()
            )) {
            event.cancel();
         }
      } else {
         event.cancel();
      }
   }

   static {
      armor.add(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
      armor.add(SoundEvents.ARMOR_EQUIP_TURTLE.value());
      armor.add(SoundEvents.ARMOR_EQUIP_CHAIN.value());
      armor.add(SoundEvents.ARMOR_EQUIP_ELYTRA.value());
      armor.add(SoundEvents.ARMOR_EQUIP_DIAMOND.value());
      armor.add(SoundEvents.ARMOR_EQUIP_GOLD.value());
      armor.add(SoundEvents.ARMOR_EQUIP_IRON.value());
      armor.add(SoundEvents.ARMOR_EQUIP_LEATHER.value());
      armor.add(SoundEvents.ARMOR_EQUIP_GENERIC.value());
      armor.add(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
      armor.add(SoundEvents.ARMOR_EQUIP_TURTLE.value());
      armor.add(SoundEvents.ARMOR_EQUIP_CHAIN.value());
      armor.add(SoundEvents.ARMOR_EQUIP_ELYTRA.value());
      armor.add(SoundEvents.ARMOR_EQUIP_DIAMOND.value());
      armor.add(SoundEvents.ARMOR_EQUIP_GOLD.value());
      armor.add(SoundEvents.ARMOR_EQUIP_IRON.value());
      armor.add(SoundEvents.ARMOR_EQUIP_LEATHER.value());
      armor.add(SoundEvents.ARMOR_EQUIP_GENERIC.value());
   }
}
