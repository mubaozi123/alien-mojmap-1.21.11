package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.TickEntityEvent;
import dev.luminous.mod.modules.impl.render.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientWorld extends Level {
   protected MixinClientWorld(
      WritableLevelData properties,
      ResourceKey<Level> registryRef,
      RegistryAccess registryManager,
      Holder<DimensionType> dimensionEntry,
      boolean isClient,
      boolean debugWorld,
      long seed,
      int maxChainedNeighborUpdates
   ) {
      super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
   }

   @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
   public void onTickEntity(Entity entity, CallbackInfo ci) {
      TickEntityEvent event = TickEntityEvent.get(entity);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
   public void onAddEntity(Entity entity, CallbackInfo ci) {
      EntitySpawnEvent event = EntitySpawnEvent.get(entity);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "removeEntity", at = @At("HEAD"))
   private void hookRemoveEntity(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      Entity entity = this.getEntity(entityId);
      if (entity != null) {
         RemoveEntityEvent removeEntityEvent = RemoveEntityEvent.get(entity, removalReason);
         Alien.EVENT_BUS.post(removeEntityEvent);
      }
   }

   @Inject(method = "addEntity", at = @At("TAIL"))
   public void onAddEntityTail(Entity entity, CallbackInfo ci) {
      EntitySpawnedEvent event = EntitySpawnedEvent.get(entity);
      Alien.EVENT_BUS.post(event);
   }

   @Override
   public float getRainLevel(float delta) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getRainLevel(delta);
   }

   @Override
   public float getThunderLevel(float delta) {
      return NoRender.INSTANCE.isOn() && NoRender.INSTANCE.weather.getValue() ? 0.0F : super.getThunderLevel(delta);
   }
}
