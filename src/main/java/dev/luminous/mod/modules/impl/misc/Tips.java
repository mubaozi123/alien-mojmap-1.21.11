package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RemoveEntityEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class Tips extends Module {
   public static Tips INSTANCE;
   public final BooleanSetting visualRange = this.add(new BooleanSetting("VisualRange", false).setParent());
   public final BooleanSetting friends = this.add(new BooleanSetting("Friends", false, this.visualRange::isOpen));
   public final BooleanSetting popCounter = this.add(new BooleanSetting("PopCounter", true));
   public final BooleanSetting deathCoords = this.add(new BooleanSetting("DeathCoords", true));
   public final BooleanSetting serverLag = this.add(new BooleanSetting("ServerLag", true));
   public final BooleanSetting lagBack = this.add(new BooleanSetting("LagBack", true));
   public final BooleanSetting potion = this.add(new BooleanSetting("Potion", true).setParent());
   public final BooleanSetting resistanceLevelCheck = this.add(new BooleanSetting("ResistanceLevelCheck", true, this.potion::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("YOffset", 0, -200, 200, this.potion::isOpen));
   final DecimalFormat df = new DecimalFormat("0.0");
   final int color = new Color(190, 0, 0).getRGB();
   private final Timer lagTimer = new Timer();
   private final Timer lagBackTimer = new Timer();
   int turtles = 0;

   public Tips() {
      super("Tips", Module.Category.Misc);
      this.setChinese("提示");
      INSTANCE = this;
   }

   @EventListener
   public void onAddEntity(EntitySpawnEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof Player && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? ChatFormatting.AQUA + playerName : ChatFormatting.WHITE + playerName) + "§f entered your visual range.",
               event.getEntity().getId() + 777
            );
            mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.visualRange.getValue() && event.getEntity() instanceof Player && event.getEntity().getDisplayName() != null) {
         String playerName = event.getEntity().getDisplayName().getString();
         boolean isFriend = Alien.FRIEND.isFriend(playerName);
         if ((!isFriend || this.friends.getValue()) && event.getEntity() != mc.player) {
            CommandManager.sendMessageId(
               (isFriend ? ChatFormatting.AQUA + playerName : ChatFormatting.WHITE + playerName) + "§f left your visual range.",
               event.getEntity().getId() + 777
            );
            mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 100.0F, 1.9F);
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.potion.getValue()) {
         this.turtles = InventoryUtil.getPotionCount(MobEffects.RESISTANCE.value());
      }
   }

   @EventListener
   public void onPacketEvent(PacketEvent.Receive event) {
      this.lagTimer.reset();
      if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.lagBackTimer.reset();
      }
   }

   @Override
   public void onRender2D(GuiGraphics drawContext, float tickDelta) {
      if (this.serverLag.getValue() && this.lagTimer.passedS(1.4)) {
         String text = "Server not responding (" + this.df.format(this.lagTimer.getMs() / 1000.0) + "s)";
         drawContext.drawString(mc.font, text, mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(text) / 2, 10 + 9, this.color, true);
      }

      if (this.lagBack.getValue() && !this.lagBackTimer.passedS(1.5)) {
         String text = "Lagback (" + this.df.format((1500L - this.lagBackTimer.getMs()) / 1000.0) + "s)";
         drawContext.drawString(mc.font, text, mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(text) / 2, 10 + 9 * 2, this.color, true);
      }

      if (this.potion.getValue()) {
         StringBuilder stringBuilder = new StringBuilder();
         if (this.turtles > 0) {
            stringBuilder.append("§e").append(this.turtles);
         }

         if (mc.player.hasEffect(MobEffects.RESISTANCE)
            && (!this.resistanceLevelCheck.getValue() || mc.player.getEffect(MobEffects.RESISTANCE).getAmplifier() > 0)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§9").append(mc.player.getEffect(MobEffects.RESISTANCE).getDuration() / 20 + 1);
         }

         if (mc.player.hasEffect(MobEffects.STRENGTH)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§4").append(mc.player.getEffect(MobEffects.STRENGTH).getDuration() / 20 + 1);
         }

         if (mc.player.hasEffect(MobEffects.SPEED)) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(" ");
            }

            stringBuilder.append("§b").append(mc.player.getEffect(MobEffects.SPEED).getDuration() / 20 + 1);
         }

         drawContext.drawString(
            mc.font,
            stringBuilder.toString(),
            mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(stringBuilder.toString()) / 2,
            mc.getWindow().getGuiScaledHeight() / 2 + 9 - this.yOffset.getValueInt(),
            -1,
            true
         );
      }
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      Player player = event.getPlayer();
      if (this.popCounter.getValue()) {
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = Alien.POP.popContainer.get(player.getName().getString());
            if (l_Count == 1) {
               if (player.equals(mc.player)) {
                  this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totem.", player.getId());
               } else {
                  this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totem.", player.getId());
               }
            } else if (player.equals(mc.player)) {
               this.sendMessage("§fYou§r died after popping §f" + l_Count + "§r totems.", player.getId());
            } else {
               this.sendMessage("§f" + player.getName().getString() + "§r died after popping §f" + l_Count + "§r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("§fYou§r died.", player.getId());
         } else {
            this.sendMessage("§f" + player.getName().getString() + "§r died.", player.getId());
         }
      }

      if (this.deathCoords.getValue() && player == mc.player) {
         this.sendMessage("§4You died at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
      }
   }

   @EventListener
   public void onTotem(TotemEvent event) {
      if (this.popCounter.getValue()) {
         Player player = event.getPlayer();
         int l_Count = 1;
         if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = Alien.POP.popContainer.get(player.getName().getString());
         }

         if (l_Count == 1) {
            if (player.equals(mc.player)) {
               this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
            } else {
               this.sendMessage("§f" + player.getName().getString() + " §rpopped §f" + l_Count + "§r totems.", player.getId());
            }
         } else if (player.equals(mc.player)) {
            this.sendMessage("§fYou§r popped §f" + l_Count + "§r totem.", player.getId());
         } else {
            this.sendMessage("§f" + player.getName().getString() + " §rhas popped §f" + l_Count + "§r totems.", player.getId());
         }
      }
   }

   public void sendMessage(String message, int id) {
      if (!nullCheck()) {
         if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
            CommandManager.sendMessageId("§f[§3" + this.getName() + "§f] " + message, id);
            return;
         }

         CommandManager.sendMessageId(message, id);
      }
   }
}
