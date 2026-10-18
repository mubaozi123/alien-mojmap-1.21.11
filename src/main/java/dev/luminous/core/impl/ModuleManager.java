package dev.luminous.core.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.impl.Render2DEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.mod.Mod;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.BaritoneModule;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import dev.luminous.mod.modules.impl.client.Fonts;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.impl.client.IRC;
import dev.luminous.mod.modules.impl.client.ItemsCounter;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.impl.combat.AntiCrawl;
import dev.luminous.mod.modules.impl.combat.AntiPhase;
import dev.luminous.mod.modules.impl.combat.AntiRegear;
import dev.luminous.mod.modules.impl.combat.AntiWeak;
import dev.luminous.mod.modules.impl.combat.Aura;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.AutoMend;
import dev.luminous.mod.modules.impl.combat.AutoPush;
import dev.luminous.mod.modules.impl.combat.AutoRegear;
import dev.luminous.mod.modules.impl.combat.AutoTrap;
import dev.luminous.mod.modules.impl.combat.AutoWeb;
import dev.luminous.mod.modules.impl.combat.Blocker;
import dev.luminous.mod.modules.impl.combat.Breaker;
import dev.luminous.mod.modules.impl.combat.Burrow;
import dev.luminous.mod.modules.impl.combat.CevBreaker;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.combat.HoleFiller;
import dev.luminous.mod.modules.impl.combat.Offhand;
import dev.luminous.mod.modules.impl.combat.PistonCrystal;
import dev.luminous.mod.modules.impl.combat.Quiver;
import dev.luminous.mod.modules.impl.combat.SelfTrap;
import dev.luminous.mod.modules.impl.combat.Surround;
import dev.luminous.mod.modules.impl.exploit.AntiHunger;
import dev.luminous.mod.modules.impl.exploit.AntiPacket;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.exploit.BowBomb;
import dev.luminous.mod.modules.impl.exploit.ChorusControl;
import dev.luminous.mod.modules.impl.exploit.ChunkESP;
import dev.luminous.mod.modules.impl.exploit.Clip;
import dev.luminous.mod.modules.impl.exploit.InfiniteTrident;
import dev.luminous.mod.modules.impl.exploit.MaceSpoof;
import dev.luminous.mod.modules.impl.exploit.NoResourcePack;
import dev.luminous.mod.modules.impl.exploit.PacketControl;
import dev.luminous.mod.modules.impl.exploit.Phase;
import dev.luminous.mod.modules.impl.exploit.PingSpoof;
import dev.luminous.mod.modules.impl.exploit.RocketExtend;
import dev.luminous.mod.modules.impl.exploit.ServerLagger;
import dev.luminous.mod.modules.impl.exploit.SkinFlicker;
import dev.luminous.mod.modules.impl.exploit.TeleportLogger;
import dev.luminous.mod.modules.impl.exploit.XCarry;
import dev.luminous.mod.modules.impl.misc.AutoEZ;
import dev.luminous.mod.modules.impl.misc.AutoKit;
import dev.luminous.mod.modules.impl.misc.AutoLog;
import dev.luminous.mod.modules.impl.misc.AutoReconnect;
import dev.luminous.mod.modules.impl.misc.BedCrafter;
import dev.luminous.mod.modules.impl.misc.Bot;
import dev.luminous.mod.modules.impl.misc.ChatAppend;
import dev.luminous.mod.modules.impl.misc.ExtraTab;
import dev.luminous.mod.modules.impl.misc.FakePlayer;
import dev.luminous.mod.modules.impl.misc.Friend;
import dev.luminous.mod.modules.impl.misc.KillEffect;
import dev.luminous.mod.modules.impl.misc.LavaFiller;
import dev.luminous.mod.modules.impl.misc.NoSound;
import dev.luminous.mod.modules.impl.misc.NoTerrainScreen;
import dev.luminous.mod.modules.impl.misc.Nuker;
import dev.luminous.mod.modules.impl.misc.PacketLogger;
import dev.luminous.mod.modules.impl.misc.Punctuation;
import dev.luminous.mod.modules.impl.misc.ShulkerViewer;
import dev.luminous.mod.modules.impl.misc.Spammer;
import dev.luminous.mod.modules.impl.misc.Tips;
import dev.luminous.mod.modules.impl.movement.AntiVoid;
import dev.luminous.mod.modules.impl.movement.AutoWalk;
import dev.luminous.mod.modules.impl.movement.BlockStrafe;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.EntityControl;
import dev.luminous.mod.modules.impl.movement.FastFall;
import dev.luminous.mod.modules.impl.movement.FastSwim;
import dev.luminous.mod.modules.impl.movement.FastWeb;
import dev.luminous.mod.modules.impl.movement.Flatten;
import dev.luminous.mod.modules.impl.movement.Fly;
import dev.luminous.mod.modules.impl.movement.HoleSnap;
import dev.luminous.mod.modules.impl.movement.MovementSync;
import dev.luminous.mod.modules.impl.movement.NoJumpDelay;
import dev.luminous.mod.modules.impl.movement.NoSlow;
import dev.luminous.mod.modules.impl.movement.PacketFly;
import dev.luminous.mod.modules.impl.movement.SafeWalk;
import dev.luminous.mod.modules.impl.movement.Scaffold;
import dev.luminous.mod.modules.impl.movement.Speed;
import dev.luminous.mod.modules.impl.movement.Sprint;
import dev.luminous.mod.modules.impl.movement.Step;
import dev.luminous.mod.modules.impl.movement.Strafe;
import dev.luminous.mod.modules.impl.movement.VClip;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.impl.player.AirPlace;
import dev.luminous.mod.modules.impl.player.AntiEffects;
import dev.luminous.mod.modules.impl.player.AutoArmor;
import dev.luminous.mod.modules.impl.player.AutoPearl;
import dev.luminous.mod.modules.impl.player.AutoPot;
import dev.luminous.mod.modules.impl.player.AutoTool;
import dev.luminous.mod.modules.impl.player.FreeLook;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.player.InteractTweaks;
import dev.luminous.mod.modules.impl.player.NoFall;
import dev.luminous.mod.modules.impl.player.PacketEat;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.player.Replenish;
import dev.luminous.mod.modules.impl.player.Sorter;
import dev.luminous.mod.modules.impl.player.TimerModule;
import dev.luminous.mod.modules.impl.player.Yaw;
import dev.luminous.mod.modules.impl.render.Ambience;
import dev.luminous.mod.modules.impl.render.AspectRatio;
import dev.luminous.mod.modules.impl.render.BreakESP;
import dev.luminous.mod.modules.impl.render.CameraClip;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.Crosshair;
import dev.luminous.mod.modules.impl.render.ESP;
import dev.luminous.mod.modules.impl.render.Fov;
import dev.luminous.mod.modules.impl.render.HighLight;
import dev.luminous.mod.modules.impl.render.HoleESP;
import dev.luminous.mod.modules.impl.render.LogoutSpots;
import dev.luminous.mod.modules.impl.render.MotionCamera;
import dev.luminous.mod.modules.impl.render.NameTags;
import dev.luminous.mod.modules.impl.render.NoRender;
import dev.luminous.mod.modules.impl.render.PhaseESP;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.impl.render.PopChams;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import dev.luminous.mod.modules.impl.render.TotemParticle;
import dev.luminous.mod.modules.impl.render.Tracers;
import dev.luminous.mod.modules.impl.render.Trajectories;
import dev.luminous.mod.modules.impl.render.ViewModel;
import dev.luminous.mod.modules.impl.render.Xray;
import dev.luminous.mod.modules.impl.render.Zoom;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.GuiGraphics;

public class ModuleManager implements Wrapper {
   private final ArrayList<Module> modules = new ArrayList<>();

   public ArrayList<Module> getModules() {
      return this.modules;
   }

   public ModuleManager() {
      this.init();
   }

   public void init() {
      if (BaritoneUtil.loaded) {
         this.addModule(new BaritoneModule());
      }

      this.addModule(new AutoKit());
      this.addModule(new Bot());
      this.addModule(new Fonts());
      this.addModule(new NoTerrainScreen());
      this.addModule(new AutoCrystal());
      this.addModule(new Ambience());
      this.addModule(new AntiHunger());
      this.addModule(new AntiVoid());
      this.addModule(new AutoWalk());
      this.addModule(new VClip());
      this.addModule(new ExtraTab());
      this.addModule(new AntiWeak());
      this.addModule(new BedCrafter());
      this.addModule(new Friend());
      this.addModule(new AspectRatio());
      this.addModule(new ChunkESP());
      this.addModule(new Aura());
      this.addModule(new PistonCrystal());
      this.addModule(new AutoAnchor());
      this.addModule(new PhaseESP());
      this.addModule(new AutoArmor());
      this.addModule(new Breaker());
      this.addModule(new AutoLog());
      this.addModule(new AutoEZ());
      this.addModule(new SelfTrap());
      this.addModule(new Sorter());
      this.addModule(new AutoMend());
      this.addModule(new AutoPot());
      this.addModule(new AutoPush());
      this.addModule(new Offhand());
      this.addModule(new Nuker());
      this.addModule(new AutoTrap());
      this.addModule(new AutoWeb());
      this.addModule(new Blink());
      this.addModule(new ChorusControl());
      this.addModule(new BlockStrafe());
      this.addModule(new FastSwim());
      this.addModule(new Blocker());
      this.addModule(new Quiver());
      this.addModule(new BowBomb());
      this.addModule(new BreakESP());
      this.addModule(new Burrow());
      this.addModule(new Punctuation());
      this.addModule(new MaceSpoof());
      this.addModule(new CameraClip());
      this.addModule(new ChatAppend());
      this.addModule(new ClickGui());
      this.addModule(new InfiniteTrident());
      this.addModule(new ColorsModule());
      this.addModule(new AutoRegear());
      this.addModule(new LavaFiller());
      this.addModule(new AntiPhase());
      this.addModule(new Clip());
      this.addModule(new AntiCheat());
      this.addModule(new IRC());
      this.addModule(new ItemsCounter());
      this.addModule(new Fov());
      this.addModule(new Criticals());
      this.addModule(new CevBreaker());
      this.addModule(new Crosshair());
      this.addModule(new Chams());
      this.addModule(new AntiPacket());
      this.addModule(new AutoReconnect());
      this.addModule(new ESP());
      this.addModule(new HoleESP());
      this.addModule(new Tracers());
      this.addModule(new MovementSync());
      this.addModule(new ElytraFly());
      this.addModule(new PacketLogger());
      this.addModule(new TeleportLogger());
      this.addModule(new SkinFlicker());
      this.addModule(new EntityControl());
      this.addModule(new NameTags());
      this.addModule(new ShulkerViewer());
      this.addModule(new PingSpoof());
      this.addModule(new FakePlayer());
      this.addModule(new Spammer());
      this.addModule(new MotionCamera());
      this.addModule(new HighLight());
      this.addModule(new FastFall());
      this.addModule(new FastWeb());
      this.addModule(new Flatten());
      this.addModule(new Fly());
      this.addModule(new Yaw());
      this.addModule(new Freecam());
      this.addModule(new FreeLook());
      this.addModule(new TimerModule());
      this.addModule(new Tips());
      this.addModule(new ClientSetting());
      this.addModule(new TextRadar());
      this.addModule(new HUD());
      this.addModule(new NoResourcePack());
      this.addModule(new RocketExtend());
      this.addModule(new HoleFiller());
      this.addModule(new HoleSnap());
      this.addModule(new LogoutSpots());
      this.addModule(new AutoTool());
      this.addModule(new Trajectories());
      this.addModule(new KillEffect());
      this.addModule(new AutoPearl());
      this.addModule(new AntiEffects());
      this.addModule(new NoFall());
      this.addModule(new NoRender());
      this.addModule(new NoSlow());
      this.addModule(new NoSound());
      this.addModule(new AirPlace());
      this.addModule(new Xray());
      this.addModule(new PacketEat());
      this.addModule(new PacketFly());
      this.addModule(new PacketMine());
      this.addModule(new PacketControl());
      this.addModule(new Phase());
      this.addModule(new PlaceRender());
      this.addModule(new InteractTweaks());
      this.addModule(new PopChams());
      this.addModule(new Replenish());
      this.addModule(new ServerLagger());
      this.addModule(new Scaffold());
      this.addModule(new ShaderModule());
      this.addModule(new AntiCrawl());
      this.addModule(new AntiRegear());
      this.addModule(new SafeWalk());
      this.addModule(new NoJumpDelay());
      this.addModule(new Speed());
      this.addModule(new Sprint());
      this.addModule(new Strafe());
      this.addModule(new Step());
      this.addModule(new Surround());
      this.addModule(new TotemParticle());
      this.addModule(new Velocity());
      this.addModule(new ViewModel());
      this.addModule(new XCarry());
      this.addModule(new Zoom());
      this.modules.sort(Comparator.comparing(Mod::getName));
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, false);
      }
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0) {
         this.handleKeyEvent(eventKey, true);
      }
   }

   private void handleKeyEvent(int key, boolean isPressed) {
      for (Module module : this.modules) {
         BindSetting bindSetting = module.getBindSetting();
         if (bindSetting.getValue() == key) {
            if (isPressed && mc.screen == null) {
               module.toggle();
               bindSetting.holding = true;
            } else if (!isPressed && bindSetting.isHoldEnable() && bindSetting.holding) {
               module.toggle();
               bindSetting.holding = false;
            }
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BindSetting bind && bind.getValue() == key) {
               bind.setPressed(isPressed);
            }
         }
      }
   }

   public void onLogin() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogin();
         }
      }
   }

   public void onLogout() {
      for (Module module : this.modules) {
         if (module.isOn()) {
            module.onLogout();
         }
      }
   }

   public void onRender2D(GuiGraphics drawContext) {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender2D(drawContext, mc.getDeltaTracker().getGameTimeDeltaTicks());
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender2D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render2DEvent.get(drawContext, mc.getDeltaTracker().getGameTimeDeltaTicks()));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }
   }

   public void render3DQueue(PoseStack matrices) {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3DQueue(matrices);
            } catch (Exception var5) {
               var5.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3DQueue]) Message: [" + var5.getMessage() + "]");
               }
            }
         }
      }
   }

   public void render3D(PoseStack matrices) {
      for (Module module : this.modules) {
         if (module.isOn()) {
            try {
               module.onRender3D(matrices);
            } catch (Exception var6) {
               var6.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred (" + module.getName() + " [onRender3D]) Message: [" + var6.getMessage() + "]");
               }
            }
         }
      }

      try {
         Alien.EVENT_BUS.post(Render3DEvent.get(matrices, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
      } catch (Exception var5) {
         var5.printStackTrace();
         if (ClientSetting.INSTANCE.debug.getValue()) {
            CommandManager.sendMessage("§4An error has occurred (Render3DEvent) Message: [" + var5.getMessage() + "]");
         }
      }

      Render3DScheduler.INSTANCE.flush(matrices);
   }

   public void addModule(Module module) {
      this.modules.add(module);
   }

   public Module getModuleByName(String string) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(string)) {
            return module;
         }
      }

      return null;
   }
}
