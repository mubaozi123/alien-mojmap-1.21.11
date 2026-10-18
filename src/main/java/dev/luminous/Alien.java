package dev.luminous;

import dev.luminous.api.events.eventbus.EventBus;
import dev.luminous.api.events.impl.InitEvent;
import dev.luminous.core.impl.BlurManager;
import dev.luminous.core.impl.BreakManager;
import dev.luminous.core.impl.CleanerManager;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.core.impl.FPSManager;
import dev.luminous.core.impl.FriendManager;
import dev.luminous.core.impl.HoleManager;
import dev.luminous.core.impl.ModuleManager;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.core.impl.PopManager;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.core.impl.ServerManager;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ThreadManager;
import dev.luminous.core.impl.TimerManager;
import dev.luminous.core.impl.TradeManager;
import dev.luminous.core.impl.XrayManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;

public class Alien implements ModInitializer {
   private static final int AES_KEY_SIZE = 256;
   private static final int GCM_IV_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 16;
   private static final int PBKDF2_ITERATIONS = 600000;
   private static final int SALT_LENGTH = 16;
   public static final String NAME = "Alien";
   public static final String VERSION = "1.21.11+build";
   public static final EventBus EVENT_BUS = new EventBus();
   public static HoleManager HOLE;
   public static PlayerManager PLAYER;
   public static TradeManager TRADE;
   public static CleanerManager CLEANER;
   public static XrayManager XRAY;
   public static ModuleManager MODULE;
   public static CommandManager COMMAND;
   public static ConfigManager CONFIG;
   public static RotationManager ROTATION;
   public static BreakManager BREAK;
   public static PopManager POP;
   public static FriendManager FRIEND;
   public static TimerManager TIMER;
   public static ShaderManager SHADER;
   public static BlurManager BLUR;
   public static FPSManager FPS;
   public static ServerManager SERVER;
   public static ThreadManager THREAD;
   public static boolean loaded = false;
   public static long initTime;
   public static String userId;
   public static final List<String> hwidCache = new ArrayList<>();

   public static String getPrefix() {
      return ClientSetting.INSTANCE.prefix.getValue();
   }

   public static void save() {
      System.out.println("[Alien] Saving");
      CONFIG.save();
      CLEANER.save();
      FRIEND.save();
      XRAY.save();
      TRADE.save();
      System.out.println("[Alien] Saved");
   }

   private void register() {
      EVENT_BUS.registerLambdaFactory((lookupInMethod, klass) -> (Lookup)lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (loaded) {
            save();
         }
      }));
   }

   @Override
   public void onInitialize() {
      this.register();
      CONFIG = new ConfigManager();
      HOLE = new HoleManager();
      MODULE = new ModuleManager();
      COMMAND = new CommandManager();
      FRIEND = new FriendManager();
      XRAY = new XrayManager();
      CLEANER = new CleanerManager();
      TRADE = new TradeManager();
      ROTATION = new RotationManager();
      BREAK = new BreakManager();
      PLAYER = new PlayerManager();
      POP = new PopManager();
      TIMER = new TimerManager();
      SHADER = new ShaderManager();
      BLUR = new BlurManager();
      FPS = new FPSManager();
      SERVER = new ServerManager();
      CONFIG.load();
      THREAD = new ThreadManager();
      initTime = System.currentTimeMillis();
      loaded = true;
      EVENT_BUS.post(new InitEvent());
      File folder = new File(Minecraft.getInstance().gameDirectory.getPath() + File.separator + "Alien".toLowerCase() + File.separator + "cfg");
      if (!folder.exists()) {
         folder.mkdirs();
      }
   }

   public static class Frame extends JFrame {
      public Frame(String hwid) {
         copyToClipboard(hwid);
         String message = "Failed verification.HWID(" + hwid + ")\n(Copied to clipboard)";
         JOptionPane.showMessageDialog(this, message, "FAILED", 2, UIManager.getIcon("OptionPane.errorIcon"));
      }

      public static void copyToClipboard(String s) {
         StringSelection selection = new StringSelection(s);
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         clipboard.setContents(selection, selection);
      }
   }
}
