package dev.luminous.mod.modules.impl.misc;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.asm.accessors.IContainerComponent;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.gui.PeekScreen;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Unique;

public class ShulkerViewer extends Module {
   private static final ItemStack[] ITEMS = new ItemStack[27];
   public static ShulkerViewer INSTANCE;
   private static int offset;
   public final BooleanSetting toolTips = this.add(new BooleanSetting("ToolTips", true));
   public final BooleanSetting icon = this.add(new BooleanSetting("Icon", true));
   private final HashMap<UUID, ShulkerViewer.Shulker> map = new HashMap<>();
   private final BooleanSetting peek = this.add(new BooleanSetting("Peek", false).setParent());
   private final SliderSetting renderTime = this.add(new SliderSetting("RenderTime", 10.0, 0.0, 100.0, 0.1, this.peek::isOpen).setSuffix("s"));
   private final SliderSetting xOffset = this.add(new SliderSetting("X", 0, 0, 1500, this.peek::isOpen));
   private final SliderSetting yOffset = this.add(new SliderSetting("Y", 120, 0, 1000, this.peek::isOpen));
   private final SliderSetting space = this.add(new SliderSetting("Space", 78.0, 0.0, 200.0, 1.0, this.peek::isOpen));

   public ShulkerViewer() {
      super("ShulkerViewer", Module.Category.Misc);
      this.setChinese("潜影盒查看");
      INSTANCE = this;
   }

   public static void renderShulkerToolTip(GuiGraphics context, int mouseX, int mouseY, ItemStack stack) {
      getItemsInContainerItem(stack, ITEMS);
      draw(context, mouseX, mouseY);
   }

   @Unique
   private static void draw(GuiGraphics context, int mouseX, int mouseY) {
      mouseX += 8;
      mouseY -= 82;
      context.fill(mouseX, mouseY, mouseX + 176, mouseY + 67, new Color(0, 0, 0, 120).getRGB());
      int row = 0;
      int i = 0;

      for (ItemStack itemStack : ITEMS) {
         context.renderItem(itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
         context.renderItemDecorations(mc.font, itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
         if (++i >= 9) {
            i = 0;
            row++;
         }
      }
   }

   public static boolean hasItems(ItemStack itemStack) {
      IContainerComponent container = (IContainerComponent)(Object)itemStack.get(DataComponents.CONTAINER);
      if (container != null && !container.getStacks().isEmpty()) {
         return true;
      } else {
         TypedEntityData<?> blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
         if (blockEntityData == null) {
            return false;
         } else {
            CompoundTag compoundTag = blockEntityData.copyTagWithoutId();
            return compoundTag != null && compoundTag.contains("Items");
         }
      }
   }

   private static ItemStack itemStackFromNbt(CompoundTag nbt) {
      if (nbt == null) {
         return ItemStack.EMPTY;
      } else {
         Provider lookup = mc.level.registryAccess();
         RegistryOps<Tag> ops = lookup.createSerializationContext(NbtOps.INSTANCE);
         DataResult<Pair<ItemStack, Tag>> decodeResult = ItemStack.CODEC.decode(ops, nbt);
         DataResult<ItemStack> result = decodeResult.map(Pair::getFirst);
         return result.result().orElse(ItemStack.EMPTY);
      }
   }

   public static void getItemsInContainerItem(ItemStack itemStack, ItemStack[] items) {
      if (itemStack.getItem() == Items.ENDER_CHEST) {
         for (int i = 0; i < Alien.PLAYER.ENDERCHEST_ITEM.size(); i++) {
            items[i] = Alien.PLAYER.ENDERCHEST_ITEM.get(i);
         }
      } else {
         Arrays.fill(items, ItemStack.EMPTY);
         DataComponentMap components = itemStack.getComponents();
         if (!components.has(DataComponents.CONTAINER)) {
            TypedEntityData<?> nbt2;
            if (components.has(DataComponents.BLOCK_ENTITY_DATA) && (nbt2 = components.get(DataComponents.BLOCK_ENTITY_DATA)).contains("Items")) {
               ListTag nbt3 = nbt2.getUnsafe().getListOrEmpty("Items");

               for (int i = 0; i < nbt3.size(); i++) {
                  CompoundTag entry = nbt3.getCompound(i).orElse(null);
                  if (entry != null) {
                     byte slot = entry.getByteOr("Slot", (byte)-1);
                     if (slot >= 0 && slot < items.length) {
                        items[slot] = itemStackFromNbt(entry);
                     }
                  }
               }
            }
         } else {
            IContainerComponent container = (IContainerComponent)(Object)components.get(DataComponents.CONTAINER);
            NonNullList<ItemStack> stacks = container.getStacks();

            for (int ix = 0; ix < stacks.size(); ix++) {
               if (ix >= 0 && ix < items.length) {
                  items[ix] = stacks.get(ix);
               }
            }
         }
      }
   }

   public static boolean openContainer(ItemStack itemStack, ItemStack[] contents, boolean pause) {
      if (!hasItems(itemStack) && itemStack.getItem() != Items.ENDER_CHEST) {
         return false;
      } else {
         getItemsInContainerItem(itemStack, contents);
         if (pause) {
            PlayerManager.screenToOpen = new PeekScreen(itemStack, contents);
         } else {
            mc.setScreen(new PeekScreen(itemStack, contents));
         }

         return true;
      }
   }

   private AbstractClientPlayer getPlayerByUuid(UUID uuid) {
      if (mc.level == null) {
         return null;
      } else {
         for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getUUID().equals(uuid)) {
               return player;
            }
         }

         return null;
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.peek.getValue()) {
         this.map.keySet().removeIf(uuid -> {
            AbstractClientPlayer p = this.getPlayerByUuid(uuid);
            if (p == null) {
               return true;
            } else {
               boolean mainHand = p.getMainHandItem().getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
               boolean offHand = p.getOffhandItem().getItem() instanceof BlockItem bix && bix.getBlock() instanceof ShulkerBoxBlock;
               return !mainHand && !offHand;
            }
         });

         for (AbstractClientPlayer player : Alien.THREAD.getPlayers()) {
            ItemStack stack = player.getMainHandItem();
            Item item = stack.getItem();
            BlockItem blockItem;
            if (!(item instanceof BlockItem) || !((blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock)) {
               stack = player.getOffhandItem();
            }

            if ((item = stack.getItem()) instanceof BlockItem && (blockItem = (BlockItem)item).getBlock() instanceof ShulkerBoxBlock) {
               ItemStack shulkerStack = stack;
               this.map.computeIfAbsent(player.getUUID(), uuid -> new ShulkerViewer.Shulker(shulkerStack, player.getGameProfile().name()));
            }
         }
      }
   }

   @Override
   public void onRender2D(GuiGraphics drawContext, float tickDelta) {
      if (this.peek.getValue()) {
         offset = 0;
         this.map.values().removeIf(shulker -> shulker.draw(drawContext));
      }
   }

   class Shulker {
      final ItemStack itemStack;
      final String name;
      private final Timer timer;

      public Shulker(ItemStack itemStack, String name) {
         this.itemStack = itemStack;
         this.timer = new Timer();
         this.name = name;
      }

      public boolean draw(GuiGraphics context) {
         if (this.timer.passedS(ShulkerViewer.this.renderTime.getValue())) {
            return true;
         } else {
            ShulkerViewer.renderShulkerToolTip(
               context, ShulkerViewer.this.xOffset.getValueInt() - 8, ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset, this.itemStack
            );
            context.drawString(
               Wrapper.mc.font,
               this.name,
               ShulkerViewer.this.xOffset.getValueInt(),
               ShulkerViewer.this.yOffset.getValueInt() + ShulkerViewer.offset - 9 - 82,
               -1,
               true
            );
            ShulkerViewer.offset = ShulkerViewer.offset + ShulkerViewer.this.space.getValueInt();
            return false;
         }
      }
   }
}
