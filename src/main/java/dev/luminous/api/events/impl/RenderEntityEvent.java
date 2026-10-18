package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class RenderEntityEvent extends Event {
   private static final RenderEntityEvent INSTANCE = new RenderEntityEvent();
   private Entity entity;
   private EntityType<?> entityType;

   private RenderEntityEvent() {
   }

   public static RenderEntityEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.entityType = null;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public static RenderEntityEvent get(EntityType<?> entityType) {
      INSTANCE.entity = null;
      INSTANCE.entityType = entityType;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public EntityType<?> getEntityType() {
      return this.entityType;
   }
}
