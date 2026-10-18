package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;

public class Chams extends Module {
   public static Chams INSTANCE;
   public final BooleanSetting crystal = this.add(new BooleanSetting("Crystal", true).setParent());
   public final BooleanSetting custom = this.add(new BooleanSetting("Custom", false, this.crystal::isOpen).setParent());
   public final BooleanSetting depth = this.add(new BooleanSetting("Depth", false, () -> this.crystal.isOpen() && this.custom.isOpen()));
   public final BooleanSetting chamsTexture = this.add(new BooleanSetting("ChamsTexture", true, () -> this.crystal.isOpen() && this.custom.isOpen()));
   public final ColorSetting fill = this.add(
      new ColorSetting("Fill", new Color(255, 255, 255, 100), () -> this.crystal.isOpen() && this.custom.isOpen()).injectBoolean(false)
   );
   public final ColorSetting line = this.add(
      new ColorSetting("Line", new Color(255, 255, 255, 100), () -> this.crystal.isOpen() && this.custom.isOpen()).injectBoolean(false)
   );
   public final ColorSetting core = this.add(new ColorSetting("Core", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
   public final ColorSetting outerFrame = this.add(new ColorSetting("OuterFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
   public final ColorSetting innerFrame = this.add(new ColorSetting("InnerFrame", new Color(255, 255, 255, 255), this.crystal::isOpen).injectBoolean(true));
   public final BooleanSetting glint = this.add(new BooleanSetting("Glint", true, this.crystal::isOpen));
   public final BooleanSetting texture = this.add(new BooleanSetting("Texture", true, this.crystal::isOpen));
   public final BooleanSetting spinSync = this.add(new BooleanSetting("SpinSync", false, this.crystal::isOpen));
   public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
   public final SliderSetting spinValue = this.add(new SliderSetting("SpinSpeed", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
   public final SliderSetting bounceHeight = this.add(new SliderSetting("BounceHeight", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
   public final SliderSetting floatValue = this.add(new SliderSetting("BounceSpeed", 1.0, 0.0, 3.0, 0.01, this.crystal::isOpen));
   public final SliderSetting floatOffset = this.add(new SliderSetting("YOffset", 0.0, -1.0, 1.0, 0.01, this.crystal::isOpen));
   public final BooleanSetting throughWall = this.add(new BooleanSetting("ThroughWall", false).setParent());
   private final BooleanSetting Crystals = this.add(new BooleanSetting("Crystals", true, this.throughWall::isOpen));
   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, this.throughWall::isOpen));
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, this.throughWall::isOpen));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, this.throughWall::isOpen));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, this.throughWall::isOpen));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, this.throughWall::isOpen));
   public final ColorSetting hand = this.add(new ColorSetting("Hand", -1).injectBoolean(true));
   public int age;

   public Chams() {
      super("Chams", Module.Category.Render);
      this.setChinese("模型上色");
      INSTANCE = this;
   }

   public boolean customCrystal() {
      return this.isOn() && this.crystal.getValue();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.age++;
   }

   @Override
   public void onRender3DQueue(PoseStack matrixStack) {
      if (this.isOn() && this.throughWall.getValue() && mc.level != null && mc.player != null) {
         SubmitNodeCollector queue = mc.gameRenderer.getSubmitNodeStorage();
         CameraRenderState cameraRenderState = mc.gameRenderer.getLevelRenderState().cameraRenderState;
         TickRateManager tickManager = mc.level.tickRateManager();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player && this.chams(entity)) {
               float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(entity));
               ModelPlayer.renderTexture(matrixStack, queue, cameraRenderState, entity, delta);
            }
         }
      }
   }

   public boolean chams(Entity entity) {
      if (entity instanceof EndCrystal) {
         return this.Crystals.getValue();
      } else if (entity instanceof Slime) {
         return this.Slimes.getValue();
      } else if (entity instanceof Player) {
         return this.Players.getValue();
      } else if (entity instanceof Villager || entity instanceof WanderingTrader) {
         return this.Villagers.getValue();
      } else if (entity instanceof Animal) {
         return this.Animals.getValue();
      } else {
         return entity instanceof Mob ? this.Mobs.getValue() : false;
      }
   }

   public int getHandArgb() {
      return this.hand.getValue().getRGB();
   }
}
