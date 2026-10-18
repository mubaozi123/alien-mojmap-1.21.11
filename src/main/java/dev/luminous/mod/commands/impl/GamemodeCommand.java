package dev.luminous.mod.commands.impl;

import dev.luminous.mod.commands.Command;
import java.util.List;
import net.minecraft.world.level.GameType;

public class GamemodeCommand extends Command {
   public GamemodeCommand() {
      super("gamemode", "[gamemode]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         if (moduleName.equalsIgnoreCase("survival")) {
            mc.gameMode.setLocalMode(GameType.SURVIVAL);
         } else if (moduleName.equalsIgnoreCase("creative")) {
            mc.gameMode.setLocalMode(GameType.CREATIVE);
         } else if (moduleName.equalsIgnoreCase("adventure")) {
            mc.gameMode.setLocalMode(GameType.ADVENTURE);
         } else if (moduleName.equalsIgnoreCase("spectator")) {
            mc.gameMode.setLocalMode(GameType.SPECTATOR);
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return count == 1 ? new String[]{"survival", "creative", "adventure", "spectator"} : null;
   }
}
