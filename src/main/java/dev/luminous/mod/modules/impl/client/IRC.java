package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.mod.modules.Module;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

public class IRC extends Module {
   public static PrintWriter printWriter;
   private static final String SERVER_HOST = "47.121.113.160";
   private static final int SERVER_PORT = 6667;
   public static volatile boolean connect;

   public IRC() {
      super("IRC", Module.Category.Client);
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.start();
      }
   }

   @EventListener
   public void onupdate(UpdateEvent event) {
      if (!connect) {
         this.start();
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundChatPacket packet) {
         String s = packet.message();
         if (s.startsWith("!")) {
            try {
               printWriter.println(s.replaceFirst("!", ""));
            } catch (Exception var5) {
            }

            event.cancel();
         }
      }
   }

   @Override
   public void onLogin() {
      this.start();
   }

   @Override
   public void onDisable() {
      try {
         connect = false;
         printWriter.println("/quit");
      } catch (Exception var2) {
      }
   }

   @Override
   public void onLogout() {
      try {
         connect = false;
         printWriter.println("/quit");
      } catch (Exception var2) {
      }
   }

   public void start() {
      connect = true;
      Alien.THREAD.execute(() -> {
         Scanner scanner = new Scanner(System.in);

         try (
            Socket socket = new Socket("47.121.113.160", 6667);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         ) {
            out.println(mc.getUser().getName() + "[" + Alien.userId + "]");
            printWriter = out;
            new Thread(() -> {
               String serverResponse;
               try {
                  while ((serverResponse = in.readLine()) != null) {
                     String finalServerResponse = "§b[IRC] §f" + serverResponse;
                     mc.execute(() -> mc.gui.getChat().addMessage(Component.nullToEmpty(finalServerResponse)));
                  }
               } catch (Exception var3x) {
                  connect = false;
               }
            }).start();

            while (connect) {
               Thread.onSpinWait();
            }
         } catch (UnknownHostException var22) {
            connect = false;
            System.err.println("Unknown host: 47.121.113.160");
         } catch (IOException var23) {
            connect = false;
            System.err.println("Couldn't connect to 47.121.113.160");
         } finally {
            connect = false;
            scanner.close();
         }
      });
   }
}
