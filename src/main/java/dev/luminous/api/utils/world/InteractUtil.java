package dev.luminous.api.utils.world;

import dev.luminous.api.utils.Wrapper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InteractUtil implements Wrapper {
   public static HitResult getRtxTarget(float yaw, float pitch, double x, double y, double z) {
      HitResult result = rayTrace(5.0, yaw, pitch, x, y, z);
      Vec3 vec3d = new Vec3(x, y, z);
      double distancePow2 = 25.0;
      if (result != null) {
         distancePow2 = result.getLocation().distanceToSqr(vec3d);
      }

      Vec3 vec3d2 = getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * 5.0, vec3d2.y * 5.0, vec3d2.z * 5.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
         mc.player,
         vec3d,
         vec3d3,
         new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3).expandTowards(vec3d2.scale(5.0)).inflate(1.0, 1.0, 1.0),
         entity -> !entity.isSpectator() && entity.isPickable(),
         distancePow2
      );
      if (entityHitResult != null) {
         Entity entity2 = entityHitResult.getEntity();
         Vec3 vec3d4 = entityHitResult.getLocation();
         double g = vec3d.distanceToSqr(vec3d4);
         if ((g < distancePow2 || result == null) && entity2 instanceof LivingEntity) {
            return entityHitResult;
         }
      }

      return result;
   }

   public static HitResult rayTrace(double dst, float yaw, float pitch, double x, double y, double z) {
      Vec3 vec3d = new Vec3(x, y, z);
      Vec3 vec3d2 = getRotationVector(pitch, yaw);
      Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
      return mc.level.clip(new ClipContext(vec3d, vec3d3, Block.OUTLINE, Fluid.NONE, mc.player));
   }

   private static Vec3 getRotationVector(float yaw, float pitch) {
      return new Vec3(
         Mth.sin(-pitch * (float) (Math.PI / 180.0)) * Mth.cos(yaw * (float) (Math.PI / 180.0)),
         -Mth.sin(yaw * (float) (Math.PI / 180.0)),
         Mth.cos(-pitch * (float) (Math.PI / 180.0)) * Mth.cos(yaw * (float) (Math.PI / 180.0))
      );
   }
}
