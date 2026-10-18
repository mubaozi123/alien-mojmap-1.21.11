package dev.luminous.mod.modules.impl.render;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.LightmapGammaEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PreRender2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class Ambience extends Module {
   public static Ambience INSTANCE;
   public final ColorSetting filter = this.add(new ColorSetting("Filter", new Color(255, 255, 255, 20)).injectBoolean(false));
   public final ColorSetting worldColor = this.add(new ColorSetting("WorldColor", new Color(-1, true)).injectBoolean(true));
   public final BooleanSetting customTime = this.add(new BooleanSetting("CustomTime", false).setParent());
   public final SliderSetting time = this.add(new SliderSetting("Time", 0, 0, 24000, this.customTime::isOpen));
   public final ColorSetting fog = this.add(new ColorSetting("FogColor", new Color(13401557)).injectBoolean(false));
   public final ColorSetting sky = this.add(new ColorSetting("SkyColor", new Color(0)).injectBoolean(false));
   public final ColorSetting cloud = this.add(new ColorSetting("CloudColor", new Color(0)).injectBoolean(false));
   public final ColorSetting dimensionColor = this.add(new ColorSetting("DimensionColor", new Color(0)).injectBoolean(false));
   public final BooleanSetting fogDistance = this.add(new BooleanSetting("FogDistance", false).setParent());
   public final SliderSetting fogStart = this.add(new SliderSetting("FogStart", 50, 0, 1000, this.fogDistance::isOpen));
   public final SliderSetting fogEnd = this.add(new SliderSetting("FogEnd", 100, 0, 1000, this.fogDistance::isOpen));
   public final BooleanSetting fullBright = this.add(new BooleanSetting("FullBright", false));
   public final BooleanSetting forceOverworld = this.add(new BooleanSetting("ForceOverworld", false));
   public final BooleanSetting customLuminance = this.add(new BooleanSetting("CustomLuminance", false).setParent().injectTask(() -> {
      if (!nullCheck()) {
         mc.levelRenderer.allChanged();
      }
   }));
   public final SliderSetting luminance = this.add(new SliderSetting("Luminance", 15, 0, 15, this.customLuminance::isOpen).injectTask(() -> {
      if (!nullCheck() && this.customLuminance.getValue()) {
         mc.levelRenderer.allChanged();
      }
   }));
   long oldTime;

   public Ambience() {
      super("Ambience", "Custom ambience", Module.Category.Render);
      this.setChinese("自定义环境");
      INSTANCE = this;
   }

   @EventListener
   public void onRender2D(PreRender2DEvent event) {
      if (this.filter.booleanValue) {
         event.drawContext.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), this.filter.getValue().getRGB());
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.customTime.getValue()) {
         mc.level.getLevelData().setDayTime((long)this.time.getValue());
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.oldTime = mc.level.getDayTime();
         if (this.customTime.getValue()) {
            mc.level.getLevelData().setDayTime((long)this.time.getValue());
         }
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         mc.level.getLevelData().setDayTime(this.oldTime);
      }
   }

   @EventListener
   public void onReceivePacket(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket && this.customTime.getValue()) {
         this.oldTime = ((ClientboundSetTimePacket)event.getPacket()).dayTime();
         event.cancel();
      }
   }

   @EventListener
   public void onLightmapGamma(LightmapGammaEvent event) {
      if (this.fullBright.getValue()) {
         event.cancel();
         event.setGamma(25);
      }
   }
}
