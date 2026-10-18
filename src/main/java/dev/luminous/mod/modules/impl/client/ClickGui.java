package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.gui.ClickGuiScreen;
import dev.luminous.mod.gui.items.Component;
import dev.luminous.mod.gui.items.Item;
import dev.luminous.mod.gui.items.buttons.Button;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));
   public final BooleanSetting font = this.add(new BooleanSetting("Font", true));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   public final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
   public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1.0, 0.0, 2.0, 0.1, this.sound::isOpen));
   public final SliderSetting height = this.add(new SliderSetting("Height", 3, 0, 7));
   public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -5.0, 5.0, 1.0));
   public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset", -1.0, -5.0, 5.0, 1.0));
   public final SliderSetting alpha = this.add(new SliderSetting("Alpha", 180, 0, 255));
   public final SliderSetting hoverAlpha = this.add(new SliderSetting("HoverAlpha", 240, 0, 255));
   public final SliderSetting topAlpha = this.add(new SliderSetting("TopAlpha", 240, 0, 255));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true).setParent());
   public final SliderSetting length = this.add(new SliderSetting("Length", 250, 0, 1000, this.fade::isOpen));
   public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.Expo, this.fade::isOpen));
   public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false).setParent());
   public final SliderSetting radius = this.add(new SliderSetting("Radius", 10.0, 0.0, 100.0, this.blur::isOpen));
   public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false).setParent().injectTask(this::a));
   public final BooleanSetting line = this.add(new BooleanSetting("Line", true, this.elements::isOpen));
   public final ColorSetting gear = this.add(new ColorSetting("Gear", -1, this.elements::isOpen).injectBoolean(false));
   public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent().injectTask(this::b));
   public final ColorSetting color = this.add(new ColorSetting("Color", this.colors::isOpen));
   public final ColorSetting hoverColor = this.add(new ColorSetting("HoverColor", -2007673515, this.colors::isOpen));
   public final ColorSetting defaultColor = this.add(new ColorSetting("DefaultColor", new Color(255, 255, 255, 0), this.colors::isOpen));
   public final ColorSetting defaultTextColor = this.add(new ColorSetting("DefaultTextColor", -1, this.colors::isOpen));
   public final ColorSetting enableTextColor = this.add(new ColorSetting("EnableTextColor", -1, this.colors::isOpen));
   public final ColorSetting topColor = this.add(new ColorSetting("TopColor", new Color(255, 0, 0), this.colors::isOpen));
   public final ColorSetting backGround = this.add(new ColorSetting("BackGround", 1996488704, this.colors::isOpen).injectBoolean(true));
   public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(0, 0, 0, 36)).injectBoolean(true));
   public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(255, 0, 0, 80), () -> this.tint.booleanValue));
   public double alphaValue;
   private final Animation animation = new Animation();
   public static String key = "";

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.setChinese("点击界面");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ClickGui.FadeOut());
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void a() {
      this.elements.setValueWithoutTask(false);
      this.elements.setOpen(!this.elements.isOpen());
   }

   public void b() {
      this.colors.setValueWithoutTask(false);
      this.colors.setOpen(!this.colors.isOpen());
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         this.updateColor();

         for (Component component : ClickGuiScreen.getInstance().getComponents()) {
            component.setHeight(18);

            for (Item item : component.getItems()) {
               item.setHeight(10 + this.height.getValueInt());
            }
         }

         mc.setScreen(ClickGuiScreen.getInstance());
      }
   }

   @Override
   public void onDisable() {
      if (mc.screen instanceof ClickGuiScreen) {
         mc.screen.onClose();
      }

      if (this.autoSave.getValue()) {
         Alien.save();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.updateColor();
      if (!(mc.screen instanceof ClickGuiScreen)) {
         this.disable();
      }
   }

   public void updateColor() {
      Button.hoverColor = this.hoverColor.getValue().getRGB();
      Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
      Button.defaultColor = this.defaultColor.getValue().getRGB();
      Button.enableTextColor = this.enableTextColor.getValue().getRGB();
   }

   public class FadeOut {
      @EventListener(priority = -99999)
      public void onRender2D(Render2DEvent event) {
         if (ClickGui.this.fade.getValue()) {
            if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
               ClickGui.this.alphaValue = ClickGui.this.animation
                  .get(ClickGui.this.isOn() ? 1.0 : 0.0, ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
            }
         } else {
            ClickGui.this.alphaValue = 1.0;
         }
      }
   }
}
