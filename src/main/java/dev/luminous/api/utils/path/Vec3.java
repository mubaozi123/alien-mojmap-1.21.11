package dev.luminous.api.utils.path;

public record Vec3(double x, double y, double z) {
   public Vec3 addVector(double x, double y, double z) {
      return new Vec3(this.x + x, this.y + y, this.z + z);
   }

   public Vec3 floor() {
      return new Vec3(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
   }

   public double squareDistanceTo(Vec3 v) {
      return Math.pow(v.x - this.x, 2.0) + Math.pow(v.y - this.y, 2.0) + Math.pow(v.z - this.z, 2.0);
   }

   public Vec3 add(Vec3 v) {
      return this.addVector(v.x(), v.y(), v.z());
   }

   public net.minecraft.world.phys.Vec3 mc() {
      return new net.minecraft.world.phys.Vec3(this.x, this.y, this.z);
   }

   @Override
   public String toString() {
      return "[" + this.x + ";" + this.y + ";" + this.z + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Vec3 vec3)) {
         return false;
      } else {
         double x1;
         try {
            x1 = vec3.x();
         } catch (Throwable var11) {
            throw new MatchException(var11.toString(), var11);
         }

         double y1 = vec3.y();
         double z1 = vec3.z();
         if (this.x != x1) {
            return false;
         } else {
            return this.y != y1 ? false : this.z == z1;
         }
      }
   }
}
