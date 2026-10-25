package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.GameLeftEvent;
import dev.luminous.api.events.impl.InventoryS2CPacketEvent;
import dev.luminous.api.events.impl.S2CCloseScreenEvent;
import dev.luminous.api.events.impl.SendMessageEvent;
import dev.luminous.api.events.impl.ServerChangePositionEvent;
import dev.luminous.mod.modules.impl.exploit.AntiPacket;
import dev.luminous.mod.modules.impl.exploit.Blink;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonPacketListenerImpl {
   @Shadow
   private ClientLevel level;
   @Unique
   private boolean alien$worldNotNull;
   @Unique
   private boolean ignore;
   @Unique
   private boolean alien$ignoreExplosion;

   protected MixinClientPlayNetworkHandler(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = "handleConfigurationStart",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      )
   )
   private void onEnterReconfiguration(ClientboundStartConfigurationPacket packet, CallbackInfo info) {
      Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
   }

   @Inject(method = "handleLogin", at = @At("HEAD"))
   private void onGameJoinHead(ClientboundLoginPacket packet, CallbackInfo info) {
      this.alien$worldNotNull = this.level != null;
   }

   @Inject(method = "handleLogin", at = @At("TAIL"))
   private void onGameJoinTail(ClientboundLoginPacket packet, CallbackInfo info) {
      if (this.alien$worldNotNull) {
         Alien.EVENT_BUS.post(GameLeftEvent.INSTANCE);
      }
   }

   @Shadow
   public abstract void sendChat(String var1);

   @Inject(
      method = "handleContainerContent",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onInventoryS2CPacket(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
      InventoryS2CPacketEvent event = InventoryS2CPacketEvent.get(packet);
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
   private void onSendChatMessage(String message, CallbackInfo ci) {
      if (!this.ignore) {
         if (message.startsWith(Alien.getPrefix())) {
            Alien.COMMAND.command(message.split(" "));
            ci.cancel();
         } else {
            SendMessageEvent event = SendMessageEvent.get(message);
            Alien.EVENT_BUS.post(event);
            if (event.isCancelled()) {
               ci.cancel();
            } else if (!event.message.equals(event.defaultMessage)) {
               this.ignore = true;
               this.sendChat(event.message);
               this.ignore = false;
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = "handleContainerClose",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onCloseScreen(ClientboundContainerClosePacket packet, CallbackInfo ci) {
      S2CCloseScreenEvent event = S2CCloseScreenEvent.get();
      Alien.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "handleExplosion", at = @At("HEAD"))
   private void velocityHook2(ClientboundExplodePacket packet, CallbackInfo ci) {
      packet.playerKnockback().ifPresent(vec3d -> {
         EntityVelocityUpdateEvent event = EntityVelocityUpdateEvent.get(Minecraft.getInstance().player, vec3d.x(), vec3d.y(), vec3d.z(), true);
         Alien.EVENT_BUS.post(event);
         if (!event.isCancelled()) {
            Minecraft.getInstance().player.addDeltaMovement(new Vec3(event.getX(), event.getY(), event.getZ()));
         }
      });
   }

   @Inject(method = "handleExplosion", at = @At("HEAD"), cancellable = true)
   private void onExplosionHead(ClientboundExplodePacket packet, CallbackInfo ci) {
      if (!this.alien$ignoreExplosion && Blink.INSTANCE.isOn() && Blink.INSTANCE.velocity.getValue() && !packet.playerKnockback().isEmpty()) {
         ci.cancel();
         ClientboundExplodePacket cleanPacket = new ClientboundExplodePacket(
            packet.center(),
            packet.radius(),
            packet.blockCount(),
            Optional.empty(),
            packet.explosionParticle(),
            packet.explosionSound(),
            packet.blockParticles()
         );
         this.alien$ignoreExplosion = true;
         ((ClientPacketListener)(Object)this).handleExplosion(cleanPacket);
         this.alien$ignoreExplosion = false;
      }
   }

   @Inject(
      method = "handleMovePlayer",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = Shift.AFTER
      ),
      cancellable = true
   )
   public void onPlayerPositionLook(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
      boolean noRotate = AntiPacket.INSTANCE.isOn() && AntiPacket.INSTANCE.s2CRotate.getValue() && Alien.SERVER.playerNull.passedS(0.25);
      if (noRotate) {
         ci.cancel();
         PacketUtils.ensureRunningOnSameThread(packet, (ClientGamePacketListener)this, this.minecraft.packetProcessor());
         LocalPlayer playerEntity = this.minecraft.player;
         Vec3 vec3d = playerEntity.getDeltaMovement();
         Vec3 changePos = packet.change().position();
         boolean bl2 = packet.relatives().contains(Relative.X);
         boolean bl22 = packet.relatives().contains(Relative.Y);
         boolean bl3 = packet.relatives().contains(Relative.Z);
         double e;
         double d;
         if (bl2) {
            d = vec3d.x();
            e = playerEntity.getX() + changePos.x;
            playerEntity.xOld = playerEntity.xOld + changePos.x;
            playerEntity.xOld = playerEntity.xOld + changePos.x;
         } else {
            d = 0.0;
            e = changePos.x;
            playerEntity.xOld = changePos.x;
            playerEntity.xOld = e;
         }

         double g;
         double f;
         if (bl22) {
            f = vec3d.y();
            g = playerEntity.getY() + changePos.y;
            playerEntity.yOld = playerEntity.yOld + changePos.y;
            playerEntity.yOld = playerEntity.yOld + changePos.y;
         } else {
            f = 0.0;
            g = changePos.y;
            playerEntity.yOld = changePos.y;
            playerEntity.yOld = g;
         }

         double i;
         double h;
         if (bl3) {
            h = vec3d.z();
            i = playerEntity.getZ() + changePos.z;
            playerEntity.zOld = playerEntity.zOld + changePos.z;
            playerEntity.zOld = playerEntity.zOld + changePos.z;
         } else {
            h = 0.0;
            i = changePos.z;
            playerEntity.zOld = changePos.z;
            playerEntity.zOld = i;
         }

         playerEntity.setPos(e, g, i);
         playerEntity.setDeltaMovement(d, f, h);
         if (AntiPacket.INSTANCE.applyYaw.getValue()) {
            float yaw = packet.change().yRot();
            float pitch = packet.change().xRot();
            if (packet.relatives().contains(Relative.X_ROT)) {
               pitch += Alien.ROTATION.getLastPitch();
            }

            if (packet.relatives().contains(Relative.Y_ROT)) {
               yaw += Alien.ROTATION.getLastYaw();
            }

            this.connection.send(new ServerboundAcceptTeleportationPacket(packet.id()));
            this.connection.send(new PosRot(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), yaw, pitch, false, false));
         } else {
            this.connection.send(new ServerboundAcceptTeleportationPacket(packet.id()));
            this.connection
               .send(
                  new PosRot(
                     playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch(), false, false
                  )
               );
         }

         Alien.EVENT_BUS.post(ServerChangePositionEvent.INSTANCE);
      }
   }
}
