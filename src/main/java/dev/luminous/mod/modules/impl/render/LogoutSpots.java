package dev.luminous.mod.modules.impl.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LogoutSpots extends Module {
   private final ColorSetting box = this.add(new ColorSetting("Box", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting chamsFill = this.add(new ColorSetting("ChamsFill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting chamsLine = this.add(new ColorSetting("ChamsLine", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final BooleanSetting renderSkin = this.add(new BooleanSetting("Texture", true));
   final Map<UUID, Player> playerCache = Maps.newConcurrentMap();
   final Map<UUID, ModelPlayer> logoutCache = Maps.newConcurrentMap();
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
   private final BooleanSetting message = this.add(new BooleanSetting("Message", true));

   public LogoutSpots() {
      super("LogoutSpots", Module.Category.Render);
      this.setChinese("退出记录");
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(Action.ADD_PLAYER)) {
               for (Entry addedPlayer : packet.newEntries()) {
                  if (addedPlayer.gameMode() != GameType.SPECTATOR) {
                     for (UUID uuid : this.logoutCache.keySet()) {
                        if (uuid.equals(addedPlayer.profile().id())) {
                           Player player = this.logoutCache.get(uuid).player;
                           if (this.message.getValue()) {
                              mc.execute(
                                 () -> this.sendMessage(
                                    "§f"
                                       + player.getName().getString()
                                       + " §rLogged back at §f"
                                       + player.getBlockX()
                                       + ", "
                                       + player.getBlockY()
                                       + ", "
                                       + player.getBlockZ()
                                 )
                              );
                           }

                           this.logoutCache.remove(uuid);
                        }
                     }
                  }
               }
            }
         } else if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket(List<UUID> var20)) {
            for (UUID uuid2 : var20) {
               for (UUID uuidx : this.playerCache.keySet()) {
                  if (uuidx.equals(uuid2)) {
                     Player player = this.playerCache.get(uuidx);
                     if (!this.logoutCache.containsKey(uuidx) && player != null) {
                        ModelPlayer modelPlayer = new ModelPlayer(player);
                        if (this.message.getValue()) {
                           mc.execute(
                              () -> this.sendMessage(
                                 "§f"
                                    + player.getName().getString()
                                    + " §rLogged out at §f"
                                    + player.getBlockX()
                                    + ", "
                                    + player.getBlockY()
                                    + ", "
                                    + player.getBlockZ()
                              )
                           );
                        }

                        this.logoutCache.put(uuidx, modelPlayer);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.playerCache.clear();
      this.logoutCache.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.playerCache.clear();

      for (AbstractClientPlayer player : Alien.THREAD.getPlayers()) {
         if (player != null && !player.equals(mc.player)) {
            this.playerCache.put(player.getGameProfile().id(), player);
         }
      }
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      for (ModelPlayer data : this.logoutCache.values()) {
         Player player = data.player;
         AABB box = ((IEntity)player).getDimensions().makeBoundingBox(player.position());
         if (this.box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, box, this.box.getValue());
         }

         if (this.fill.booleanValue) {
            Render3DUtil.drawFill(matrixStack, box, this.fill.getValue());
         }

         if (this.chamsFill.booleanValue || this.chamsLine.booleanValue) {
            data.render(matrixStack, this.chamsFill, this.chamsLine);
         }

         if (this.text.booleanValue) {
            Render3DUtil.drawText3D(
               player.getName().getString()
                  + (this.health.getValue() ? TextRadar.getHealthColor(player) + " " + round2(player.getHealth() + player.getAbsorptionAmount()) : "")
                  + (
                     this.totem.getValue() && Alien.POP.getPop(player) > 0
                        ? TextRadar.getPopColor(Alien.POP.getPop(player)) + " -" + Alien.POP.getPop(player)
                        : ""
                  ),
               new Vec3(player.getX(), ((IEntity)player).getDimensions().makeBoundingBox(player.position()).maxY + 0.5, player.getZ()),
               this.text.getValue()
            );
         }
      }
   }

   @Override
   public void onRender3DQueue(PoseStack matrixStack) {
      if (this.renderSkin.getValue() && !this.logoutCache.isEmpty()) {
         SubmitNodeCollector queue = mc.gameRenderer.getSubmitNodeStorage();
         CameraRenderState cameraRenderState = mc.gameRenderer.getLevelRenderState().cameraRenderState;
         float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

         for (ModelPlayer data : this.logoutCache.values()) {
            data.renderPlayerSkin(matrixStack, queue, cameraRenderState, delta);
         }
      }
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(1, RoundingMode.HALF_UP);
      return bd.floatValue();
   }
}
