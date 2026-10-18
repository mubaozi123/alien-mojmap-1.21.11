package dev.luminous.api.utils.path;

import com.google.common.collect.Lists;
import dev.luminous.api.utils.Wrapper;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public class TPUtils implements Wrapper {
   public static void teleportWithBack(net.minecraft.world.phys.Vec3 newPos, TPUtils.TeleportType type, Runnable runnable) {
      switch (type) {
         case Legacy:
            legacyTeleportWithBack(newPos, runnable);
            break;
         case New:
            newTeleportWithBack(newPos, runnable);
      }
   }

   public static void legacyTeleportWithBack(net.minecraft.world.phys.Vec3 newPos, Runnable runnable) {
      List<Vec3> tpPath = PathUtils.computePath(newPos);
      tpPath.removeFirst();
      tpPath.forEach(vec3 -> mc.player.connection.send(new Pos(vec3.x(), vec3.y(), vec3.z(), false, false)));
      runnable.run();
      tpPath = Lists.reverse(tpPath);
      tpPath.removeFirst();
      tpPath.forEach(vec3 -> mc.player.connection.send(new Pos(vec3.x(), vec3.y(), vec3.z(), false, false)));
      mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
   }

   public static void newTeleportWithBack(net.minecraft.world.phys.Vec3 newPos, Runnable runnable) {
      int packetsRequired = (int)Math.ceil(mc.player.position().distanceTo(newPos) / 10.0) - 1;

      for (int i = 0; i < packetsRequired; i++) {
         mc.player.connection.send(new StatusOnly(true, false));
      }

      mc.player.connection.send(new Pos(newPos.x, newPos.y, newPos.z, true, false));
      runnable.run();

      for (int var4 = 0; var4 < packetsRequired; var4++) {
         mc.player.connection.send(new StatusOnly(true, false));
      }

      mc.player.connection.send(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
   }

   public static void newTeleport(net.minecraft.world.phys.Vec3 newPos) {
      int packetsRequired = (int)Math.ceil(mc.player.position().distanceTo(newPos) / 10.0) - 1;

      for (int i = 0; i < packetsRequired; i++) {
         mc.player.connection.send(new StatusOnly(true, false));
      }

      mc.player.connection.send(new Pos(newPos.x, newPos.y, newPos.z, true, false));
   }

   public static enum TeleportType {
      Legacy,
      New;
   }
}
