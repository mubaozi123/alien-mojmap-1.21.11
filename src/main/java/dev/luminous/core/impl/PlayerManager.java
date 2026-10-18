package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.BlockActivateEvent;
import dev.luminous.api.events.impl.GameLeftEvent;
import dev.luminous.api.events.impl.OpenScreenEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.mod.modules.Module;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.phys.AABB;

public class PlayerManager implements Wrapper {
   public static Screen screenToOpen;
   public final NonNullList<ItemStack> ENDERCHEST_ITEM = NonNullList.withSize(27, ItemStack.EMPTY);
   public final Map<Player, PlayerManager.EntityAttribute> map = new ConcurrentHashMap<>();
   public final CopyOnWriteArrayList<Player> inWebPlayers = new CopyOnWriteArrayList<>();
   public boolean known = false;
   private int echestOpenedState;

   public PlayerManager() {
      Alien.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onLogout(GameLeftEvent event) {
      this.inWebPlayers.clear();
      this.map.clear();
      this.ENDERCHEST_ITEM.clear();
      this.known = false;
   }

   @EventListener
   private void onBlockActivate(BlockActivateEvent event) {
      if (event.blockState.getBlock() instanceof EnderChestBlock && this.echestOpenedState == 0) {
         this.echestOpenedState = 1;
      }
   }

   @EventListener
   private void onOpenScreenEvent(OpenScreenEvent event) {
      if (this.echestOpenedState == 1 && event.screen instanceof ContainerScreen) {
         this.echestOpenedState = 2;
      } else if (this.echestOpenedState != 0) {
         if (mc.screen instanceof ContainerScreen) {
            ChestMenu container = ((ContainerScreen)mc.screen).getMenu();
            if (container != null) {
               Container inv = container.getContainer();

               for (int i = 0; i < 27; i++) {
                  this.ENDERCHEST_ITEM.set(i, inv.getItem(i));
               }

               this.known = true;
               this.echestOpenedState = 0;
            }
         }
      }
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         if (screenToOpen != null && mc.screen == null) {
            mc.setScreen(screenToOpen);
            screenToOpen = null;
         }

         this.inWebPlayers.clear();

         for (Player playerEntity : Alien.THREAD.getPlayers()) {
            this.map
               .put(playerEntity, new PlayerManager.EntityAttribute(playerEntity.getArmorValue(), playerEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
            this.webUpdate(playerEntity);
         }
      }
   }

   public boolean isInWeb(Player player) {
      return this.inWebPlayers.contains(player);
   }

   private void webUpdate(Player player) {
      for (float x : new float[]{0.0F, 0.3F, -0.3F}) {
         for (float z : new float[]{0.0F, 0.3F, -0.3F}) {
            for (int y : new int[]{-1, 0, 1, 2}) {
               BlockPos pos = new BlockPosX(player.getX() + x, player.getY(), player.getZ() + z).above(y);
               if (new AABB(pos).intersects(player.getBoundingBox()) && mc.level.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                  this.inWebPlayers.add(player);
                  return;
               }
            }
         }
      }
   }

   public record EntityAttribute(int armor, double toughness) {
   }
}
