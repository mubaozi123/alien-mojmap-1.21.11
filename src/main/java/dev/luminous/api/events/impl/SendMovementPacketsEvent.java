package dev.luminous.api.events.impl;

public class SendMovementPacketsEvent {
   public static final SendMovementPacketsEvent instance = new SendMovementPacketsEvent();
   private float originalYaw;
   private float originalPitch;
   private float yaw;
   private float pitch;

   private SendMovementPacketsEvent() {
   }

   public static SendMovementPacketsEvent get(float yaw, float pitch) {
      instance.originalYaw = yaw;
      instance.originalPitch = pitch;
      instance.yaw = yaw;
      instance.pitch = pitch;
      return instance;
   }

   public boolean isModified() {
      return Float.compare(this.yaw, this.originalYaw) != 0 || Float.compare(this.pitch, this.originalPitch) != 0;
   }

   public float getOriginalYaw() {
      return this.originalYaw;
   }

   public float getOriginalPitch() {
      return this.originalPitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
