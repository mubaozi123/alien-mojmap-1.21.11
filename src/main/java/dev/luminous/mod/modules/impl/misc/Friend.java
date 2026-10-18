package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.setChinese("好友");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player player) {
            Alien.FRIEND.friend(player);
         }

         this.disable();
      }
   }
}
