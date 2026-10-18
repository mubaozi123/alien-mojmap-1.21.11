package dev.luminous.core.impl;

import dev.luminous.core.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.world.item.Items;
import org.apache.commons.io.IOUtils;

public class CleanerManager extends Manager {
   private final ArrayList<String> list = new ArrayList<>();

   public CleanerManager() {
      this.read();
   }

   public ArrayList<String> getList() {
      return this.list;
   }

   public boolean inList(String name) {
      return this.list.contains(name) || this.list.contains(name.replace("block.minecraft.", "").replace("item.minecraft.", ""));
   }

   public void clear() {
      this.list.clear();
   }

   public void remove(String name) {
      name = name.replace("block.minecraft.", "").replace("item.minecraft.", "");
      this.list.remove(name);
   }

   public void add(String name) {
      if (!this.list.contains(name = name.replace("block.minecraft.", "").replace("item.minecraft.", ""))) {
         this.list.add(name);
      }
   }

   public void read() {
      try {
         File friendFile = getFile("cleaner.txt");
         if (!friendFile.exists()) {
            this.add(Items.NETHERITE_SWORD.getDescriptionId());
            this.add(Items.NETHERITE_PICKAXE.getDescriptionId());
            this.add(Items.NETHERITE_HELMET.getDescriptionId());
            this.add(Items.NETHERITE_CHESTPLATE.getDescriptionId());
            this.add(Items.NETHERITE_LEGGINGS.getDescriptionId());
            this.add(Items.NETHERITE_BOOTS.getDescriptionId());
            this.add(Items.OBSIDIAN.getDescriptionId());
            this.add(Items.ENDER_CHEST.getDescriptionId());
            this.add(Items.ENDER_PEARL.getDescriptionId());
            this.add(Items.ENCHANTED_GOLDEN_APPLE.getDescriptionId());
            this.add(Items.EXPERIENCE_BOTTLE.getDescriptionId());
            this.add(Items.COBWEB.getDescriptionId());
            this.add(Items.POTION.getDescriptionId());
            this.add(Items.SPLASH_POTION.getDescriptionId());
            this.add(Items.TOTEM_OF_UNDYING.getDescriptionId());
            this.add(Items.END_CRYSTAL.getDescriptionId());
            this.add(Items.ELYTRA.getDescriptionId());
            this.add(Items.FLINT_AND_STEEL.getDescriptionId());
            this.add(Items.PISTON.getDescriptionId());
            this.add(Items.STICKY_PISTON.getDescriptionId());
            this.add(Items.REDSTONE_BLOCK.getDescriptionId());
            this.add(Items.GLOWSTONE.getDescriptionId());
            this.add(Items.RESPAWN_ANCHOR.getDescriptionId());
            this.add(Items.ANVIL.getDescriptionId());
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
            this.add(s);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   public void save() {
      try {
         File friendFile = getFile("cleaner.txt");
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

         for (String str : this.list) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }
}
