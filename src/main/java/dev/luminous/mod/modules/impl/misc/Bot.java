package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.events.impl.UpdateRotateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.core.impl.RotationManager;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.combat.Aura;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

public class Bot extends Module {
   public static Bot INSTANCE;
   public final EnumSetting<Bot.Mode> mode = this.add(new EnumSetting<>("Mode", Bot.Mode.AutoTrade));
   private final BooleanSetting autoEat = this.add(new BooleanSetting("AutoEat", true).setParent());
   private final SliderSetting hunger = this.add(new SliderSetting("Hunger", 10.0, 0.0, 20.0, 1.0, this.autoEat::isOpen));
   private final SliderSetting health = this.add(new SliderSetting("Health", 20.0, 0.0, 36.0, 0.1, this.autoEat::isOpen));
   private final BooleanSetting anyFood = this.add(new BooleanSetting("AnyFood", false, this.autoEat::isOpen));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5, 0, 100, () -> this.mode.is(Bot.Mode.TridentDupe)));
   private final BooleanSetting dropTridents = this.add(new BooleanSetting("DropTridents", true, () -> this.mode.is(Bot.Mode.TridentDupe)));
   private final BooleanSetting durabilityManagement = this.add(new BooleanSetting("DurabilityManagement", true, () -> this.mode.is(Bot.Mode.TridentDupe)));
   public final BooleanSetting edit = this.add(new BooleanSetting("Edit", false, () -> this.mode.is(Bot.Mode.AutoTrade)).injectTask(this::openAutoTradeEdit));
   public final SliderSetting repeatSetting = this.add(new SliderSetting("Repeat", 2.0, 1.0, 15.0, 1.0, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting autoCloseSetting = this.add(new BooleanSetting("AutoClose", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting timeoutCloseSetting = this.add(new BooleanSetting("TimeoutClose", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final SliderSetting timeOutSetting = this.add(new SliderSetting("Timeout", 1.0, 0.0, 15.0, 0.1, () -> this.mode.is(Bot.Mode.AutoTrade)));
   public final BooleanSetting autoOpenSetting = this.add(new BooleanSetting("AutoOpen", true, () -> this.mode.is(Bot.Mode.AutoTrade)));
   private final SliderSetting range = this.add(
      new SliderSetting("Range", 4.0, 0.0, 8.0, 0.1, () -> this.mode.is(Bot.Mode.SlabPlacer) || this.mode.is(Bot.Mode.AutoTrade))
   );
   private final BooleanSetting inventory = this.add(new BooleanSetting("Inventory", true, () -> this.mode.is(Bot.Mode.NPlusOneDupe)));
   private final BooleanSetting ai = this.add(new BooleanSetting("AI", true, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final BooleanSetting nuker = this.add(new BooleanSetting("Nuker", true, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final BooleanSetting redSand = this.add(new BooleanSetting("RedSand", false, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final SliderSetting breaks = this.add(new SliderSetting("Breaks", 10, 0, 20, () -> this.mode.is(Bot.Mode.SandMiner)));
   private final SliderSetting maxTime = this.add(new SliderSetting("MaxTime", 60, 0, 100, () -> this.mode.is(Bot.Mode.Ominous)));
   public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 1, 1, 8, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, () -> this.mode.is(Bot.Mode.SlabPlacer)));
   private final StringSetting name = this.add(new StringSetting("Name", "KizuatoResu", () -> this.mode.is(Bot.Mode.ScoreFarmer)));
   private final BooleanSetting getScore = this.add(new BooleanSetting("GetScore", false, () -> this.mode.is(Bot.Mode.ScoreFarmer)));
   private final Timer commandTimer = new Timer();
   private final Timer duelItemTimer = new Timer();
   final List<BlockPos> emptyBox = new ArrayList<>();
   final List<Turtle> inLove = new ArrayList<>();
   final Timer timeOut = new Timer();
   final Timer closeScreen = new Timer();
   final Timer openTimeOut = new Timer();
   final Timer putTimer = new Timer();
   final Timer ominousTimer = new Timer();
   Bot.Stage stage = Bot.Stage.Open;
   BlockPos boxPos;
   boolean closeToBox;
   boolean putIn;
   Llama llama;
   boolean storageSand = false;
   boolean endEat = false;
   final Timer craftTimer = new Timer();
   final Timer screenTimeout = new Timer();
   int lastSlot = -1;
   int tick = 0;
   private final List<Villager> tradedVillager = new ArrayList<>();
   private final Timer timeoutTimer = new Timer();
   int placeProgress = 0;
   private final Timer slabPlacerDelay = new Timer();
   private boolean cancel = true;
   private final List<Tuple<Long, Runnable>> scheduledTasks = new ArrayList<>();
   private final List<Tuple<Long, Runnable>> scheduledTasks2 = new ArrayList<>();

   public Bot() {
      super("Bot", Module.Category.Misc);
      this.setChinese("机器人");
      INSTANCE = this;
   }

   private void openAutoTradeEdit() {
      this.edit.setValueWithoutTask(false);
      if (!nullCheck()) {
         mc.setScreen(new WindowsScreen(new ItemSelectWindow(Alien.TRADE)));
      }
   }

   private void placeBlock(BlockPos pos) {
      int block;
      if ((block = this.getBlock()) != -1) {
         int oldSlot = mc.player.getInventory().selected;
         if (BlockUtil.canPlace(pos)) {
            if (BlockUtil.allowAirPlace()) {
               this.doSwap(block);
               BlockUtil.placedPos.add(pos);
               BlockUtil.airPlace(pos, this.rotate.getValue());
               if (this.inventory.getValue()) {
                  this.doSwap(block);
                  EntityUtil.syncInventory();
               } else {
                  this.doSwap(oldSlot);
               }

               return;
            }

            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) {
               return;
            }

            this.doSwap(block);
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.relative(side), side.getOpposite(), this.rotate.getValue());
            if (this.inventory.getValue()) {
               this.doSwap(block);
               EntityUtil.syncInventory();
            } else {
               this.doSwap(oldSlot);
            }
         }
      }
   }

   private void tryPlaceBlock(BlockPos pos) {
      if (pos != null) {
         if (this.placeProgress < this.blocksPer.getValue()) {
            int block;
            if (this.inventory.getValue()) {
               block = InventoryUtil.findClassInventorySlot(SlabBlock.class);
            } else {
               block = InventoryUtil.findClass(SlabBlock.class);
            }

            if (block != -1) {
               Direction side = BlockUtil.getPlaceSide(pos);
               if (side != null) {
                  Vec3 directionVec = new Vec3(
                     pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5,
                     pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5,
                     pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5
                  );
                  if (BlockUtil.canPlace(pos, 6.0, true)) {
                     if (this.rotate.getValue()) {
                        Alien.ROTATION.lookAt(directionVec);
                     }

                     if (!BlockUtil.hasEntity(pos, false)) {
                        int old = mc.player.getInventory().selected;
                        this.doSwap(block);
                        BlockUtil.placedPos.add(pos);
                        if (BlockUtil.allowAirPlace()) {
                           BlockUtil.airPlace(pos, false, InteractionHand.MAIN_HAND, true);
                        } else {
                           BlockUtil.clickBlock(pos.relative(side), side.getOpposite(), false, InteractionHand.MAIN_HAND);
                        }

                        if (this.inventory.getValue()) {
                           this.doSwap(block);
                           EntityUtil.syncInventory();
                        } else {
                           this.doSwap(old);
                        }

                        if (this.rotate.getValue()) {
                           Alien.ROTATION.snapBack();
                        }

                        this.placeProgress++;
                        this.slabPlacerDelay.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private void doSwap(int slot) {
      if (this.inventory.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selected);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getBlock() {
      return this.inventory.getValue() ? InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class) : InventoryUtil.findClass(ShulkerBoxBlock.class);
   }

   @Override
   public String getInfo() {
      return this.mode.is(Bot.Mode.XinDupe) ? "Stage:" + this.stage.name() + ", Riding:" + mc.player.isPassenger() : this.mode.getValue().name();
   }

   @Override
   public void onEnable() {
      this.emptyBox.clear();
      this.storageSand = false;
      this.stage = Bot.Stage.Summon;
      this.boxPos = null;
      this.closeToBox = false;
      this.llama = null;
      this.putIn = false;
      this.tick = 0;
      this.scheduledTasks.clear();
      this.scheduledTasks2.clear();
      if (this.mode.is(Bot.Mode.TridentDupe)) {
         this.tridentDupe();
      }
   }

   @EventListener(priority = 201)
   private void onSendPacket(PacketEvent.Send event) {
      if (this.mode.is(Bot.Mode.TridentDupe)) {
         if (!this.cancel) {
            return;
         }

         if (event.getPacket() instanceof ServerboundMovePlayerPacket || event.getPacket() instanceof ServerboundContainerClosePacket) {
            return;
         }

         if (!(event.getPacket() instanceof ServerboundContainerClickPacket) && !(event.getPacket() instanceof ServerboundPlayerActionPacket)) {
            return;
         }

         event.cancel();
      }
   }

   private void tridentDupe() {
      int delayInt = this.delay.getValueInt() * 100;
      int lowestHotbarSlot = 0;
      int lowestHotbarDamage = 1000;

      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).getItem() == Items.TRIDENT) {
            int currentHotbarDamage = mc.player.getInventory().getItem(i).getDamageValue();
            if (lowestHotbarDamage > currentHotbarDamage) {
               lowestHotbarSlot = i;
               lowestHotbarDamage = currentHotbarDamage;
            }
         }
      }

      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      this.cancel = true;
      int finalLowestHotbarSlot = lowestHotbarSlot;
      this.scheduleTask(() -> {
         this.cancel = false;
         if (this.durabilityManagement.getValue() && finalLowestHotbarSlot != 0) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 44, 0, ClickType.SWAP, mc.player);
            if (this.dropTridents.getValue()) {
               mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 44, 0, ClickType.THROW, mc.player);
            }

            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 36 + finalLowestHotbarSlot, 0, ClickType.SWAP, mc.player);
         }

         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 3, 0, ClickType.SWAP, mc.player);
         ServerboundPlayerActionPacket packet2 = new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN, 0);
         mc.getConnection().send(packet2);
         if (this.dropTridents.getValue()) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 44, 0, ClickType.THROW, mc.player);
         }

         this.cancel = true;
         this.scheduleTask2(this::tridentDupe, delayInt);
      }, delayInt);
   }

   public void scheduleTask(Runnable task, long delayMillis) {
      long executeTime = System.currentTimeMillis() + delayMillis;
      this.scheduledTasks.add(new Tuple<>(executeTime, task));
   }

   public void scheduleTask2(Runnable task, long delayMillis) {
      long executeTime = System.currentTimeMillis() + delayMillis;
      this.scheduledTasks2.add(new Tuple<>(executeTime, task));
   }

   @Override
   public void onDisable() {
      BaritoneUtil.cancelEverything();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (nullCheck()) {
         this.emptyBox.clear();
         this.stage = Bot.Stage.Summon;
         this.boxPos = null;
         this.closeToBox = false;
         this.llama = null;
         this.putIn = false;
      } else if (!this.autoEat.getValue()
         || !(mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.health.getValueFloat())
            && mc.player.getFoodData().getFoodLevel() >= this.hunger.getValueInt()) {
         if (this.endEat) {
            this.endEat = false;
            mc.options.keyUse.setDown(false);
         }

         switch ((Bot.Mode)this.mode.getValue()) {
            case AutoTrade: {
               if (mc.player.containerMenu instanceof MerchantMenu handler) {
                  if (this.timeoutCloseSetting.getValue() && this.timeoutTimer.passedS(this.timeOutSetting.getValue())) {
                     mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                     mc.screen.onClose();
                     return;
                  }

                  int i = 0;
                  MerchantOffers list = handler.getOffers();

                  for (int size = 0; size < list.size(); size++) {
                     if (i >= this.repeatSetting.getValue()) {
                        return;
                     }

                     MerchantOffer tradeOffer = list.get(size);
                     if (!tradeOffer.isOutOfStock() && Alien.TRADE.inWhitelist(tradeOffer.getResult().getItem().getDescriptionId())) {
                        while (i < this.repeatSetting.getValue()) {
                           if (!tradeOffer.getCostA().isEmpty()) {
                              int count = InventoryUtil.getItemCount(tradeOffer.getCostA().getItem());
                              if (handler.getSlot(0).getItem().getItem() == tradeOffer.getCostA().getItem()) {
                                 count += handler.getSlot(0).getItem().getCount();
                              }

                              if (count < tradeOffer.getCostA().getCount()) {
                                 break;
                              }
                           }

                           if (!tradeOffer.getCostB().isEmpty()) {
                              int countx = InventoryUtil.getItemCount(tradeOffer.getCostB().getItem());
                              if (handler.getSlot(1).getItem().getItem() == tradeOffer.getCostB().getItem()) {
                                 countx += handler.getSlot(1).getItem().getCount();
                              }

                              if (countx < tradeOffer.getCostB().getCount()) {
                                 break;
                              }
                           }

                           mc.getConnection().send(new ServerboundSelectTradePacket(size));
                           mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 2, 1, ClickType.QUICK_MOVE, mc.player);
                           i++;
                        }
                     }
                  }

                  if (this.autoCloseSetting.getValue() && i < this.repeatSetting.getValue()) {
                     mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                     mc.screen.onClose();
                  }
               } else {
                  this.timeoutTimer.reset();
                  if (this.autoOpenSetting.getValue()) {
                     for (Entity entityxxxx : Alien.THREAD.getEntities()) {
                        if (entityxxxx instanceof Villager villager) {
                           if (mc.player.distanceTo(villager) <= this.range.getValue()) {
                              if (!this.tradedVillager.contains(villager)) {
                                 this.tradedVillager.add(villager);
                                 if (this.rotate.getValue()) {
                                    Alien.ROTATION.snapAt(villager.getEyePosition());
                                 }

                                 mc.getConnection()
                                    .send(ServerboundInteractPacket.createInteractionPacket(villager, mc.player.isShiftKeyDown(), InteractionHand.MAIN_HAND));
                                 if (this.rotate.getValue()) {
                                    Alien.ROTATION.snapBack();
                                 }

                                 return;
                              }
                           } else {
                              this.tradedVillager.remove(villager);
                           }
                        }
                     }
                  }
               }
               break;
            }
            case ItemFrameDupe: {
               this.tick++;
               int shulkerx = InventoryUtil.findClass(ShulkerBoxBlock.class);
               if (shulkerx != -1) {
                  for (Entity entityxxx : Alien.THREAD.getEntities()) {
                     if (entityxxx instanceof ItemFrame itemFrameEntity && !(entityxxx.distanceTo(mc.player) > 3.0F)) {
                        InventoryUtil.switchToSlot(shulkerx);
                        mc.getConnection().send(ServerboundInteractPacket.createInteractionPacket(itemFrameEntity, false, InteractionHand.MAIN_HAND));
                        if (this.tick >= 2) {
                           mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(itemFrameEntity, false));
                           mc.player.swing(InteractionHand.MAIN_HAND);
                           this.tick = 0;
                        }

                        return;
                     }
                  }
               }

               this.tick = 0;
               break;
            }
            case XinDupe: {
               if (this.llama != null && (this.llama.isDeadOrDying() || this.llama.distanceTo(mc.player) > 20.0F)) {
                  this.llama = null;
               }

               int chestSlot = InventoryUtil.findBlockInventorySlot(Blocks.CHEST);
               int swordSlot = findSwordSlot();
               if (chestSlot == -1) {
                  this.emptyBox.clear();
                  this.stage = Bot.Stage.Open;
                  this.boxPos = null;
                  this.closeToBox = false;
                  this.llama = null;
                  this.putIn = false;
                  return;
               }

               int shulkers = 0;

               for (Entry<Integer, ItemStack> entry : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                  if (entry.getValue().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                     shulkers++;
                  }
               }

               if (shulkers > 18) {
                  if (mc.screen != null) {
                     mc.screen.onClose();
                  }

                  for (int slot1x = 9; slot1x < 36; slot1x++) {
                     ItemStack stack = mc.player.getInventory().getItem(slot1x);
                     if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                        shulkers--;
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot1x, 1, ClickType.THROW, mc.player);
                        if (shulkers <= 18) {
                           return;
                        }
                     }
                  }

                  return;
               }

               if (this.closeToBox && this.boxPos != null) {
                  this.closeTo(this.boxPos);
               }

               if (!this.openTimeOut.passed(100L)) {
                  return;
               }

               switch (this.stage) {
                  case Open: {
                     if (!this.closeScreen.passed(250L)) {
                        if (mc.screen != null) {
                           mc.screen.onClose();
                        }

                        return;
                     }

                     if (mc.player.isPassenger()) {
                        mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, true)));
                        return;
                     }

                     if (this.boxPos == null || this.emptyBox.contains(this.boxPos)) {
                        for (BlockPos posxxxx : BlockUtil.getSphere(3.0F)) {
                           if (!this.emptyBox.contains(posxxxx)
                              && mc.level.getBlockEntity(posxxxx) instanceof ShulkerBoxBlockEntity
                              && BlockUtil.getClickSideStrict(posxxxx) != null) {
                              this.closeToBox = false;
                              this.boxPos = posxxxx;
                              break;
                           }
                        }
                     }

                     if (this.boxPos != null && !this.emptyBox.contains(this.boxPos)) {
                        if (mc.player.getEyePosition().distanceTo(this.boxPos.getCenter()) < 4.0) {
                           if (this.openTimeOut.passedS(1.0)) {
                              this.closeToBox = false;
                              this.openTimeOut.reset();
                              BlockUtil.clickBlock(this.boxPos, BlockUtil.getClickSide(this.boxPos), true);
                              this.stage = Bot.Stage.Take;
                              return;
                           }
                        } else {
                           this.closeToBox = true;
                        }

                        return;
                     }

                     return;
                  }
                  case Take: {
                     if (mc.player.isPassenger()) {
                        mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, true)));
                        return;
                     }

                     if (this.boxPos == null || this.emptyBox.contains(this.boxPos)) {
                        this.closeScreen.reset();
                        this.stage = Bot.Stage.Open;
                        return;
                     }

                     if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerx) {
                        boolean egg = false;
                        boolean hay = false;

                        for (Slot slotxxx : shulkerx.slots) {
                           if (slotxxx.index < 27 && !slotxxx.getItem().isEmpty()) {
                              if (slotxxx.getItem().getItem() == Items.EGG) {
                                 egg = true;
                              }

                              if (slotxxx.getItem().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                 hay = true;
                              }
                           }
                        }

                        if (egg && hay) {
                           int eggs = 0;
                           int hays = 0;

                           for (Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                              if (entryx.getValue().getItem() == Items.EGG) {
                                 eggs++;
                              }

                              if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                                 hays++;
                              }
                           }

                           for (Slot slotxxxx : shulkerx.slots) {
                              if (!slotxxxx.getItem().isEmpty()) {
                                 if (slotxxxx.index < 27) {
                                    if (slotxxxx.getItem().getItem() == Items.EGG && eggs < 2) {
                                       eggs++;
                                       mc.gameMode.handleInventoryMouseClick(shulkerx.containerId, slotxxxx.index, 0, ClickType.QUICK_MOVE, mc.player);
                                    }

                                    if (slotxxxx.getItem().getItem() == Blocks.HAY_BLOCK.asItem() && hays < 2) {
                                       hays++;
                                       mc.gameMode.handleInventoryMouseClick(shulkerx.containerId, slotxxxx.index, 0, ClickType.QUICK_MOVE, mc.player);
                                    }
                                 } else if (slotxxxx.getItem().getItem() == Items.LEATHER) {
                                    mc.gameMode.handleInventoryMouseClick(shulkerx.containerId, slotxxxx.index, 0, ClickType.QUICK_MOVE, mc.player);
                                 }
                              }
                           }

                           if (hays < 1 || eggs < 1) {
                              this.emptyBox.add(this.boxPos);
                           }

                           if (mc.screen != null) {
                              mc.screen.onClose();
                           }

                           this.stage = Bot.Stage.Summon;
                        } else {
                           this.closeScreen.reset();
                           this.emptyBox.add(this.boxPos);
                           this.stage = Bot.Stage.Open;
                        }

                        return;
                     } else {
                        if (this.openTimeOut.passedS(1.0)) {
                           this.closeScreen.reset();
                           this.stage = Bot.Stage.Open;
                           return;
                        }

                        return;
                     }
                  }
                  case Summon: {
                     int eggs = 0;
                     int hays = 0;

                     for (Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                        if (entryx.getValue().getItem() == Items.EGG) {
                           eggs++;
                        }

                        if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                           hays++;
                        }
                     }

                     if (eggs <= 1 || hays <= 1) {
                        this.closeScreen.reset();
                        this.stage = Bot.Stage.Open;
                        return;
                     }

                     for (Entity entityxxxxx : Alien.THREAD.getEntities()) {
                        if (entityxxxxx instanceof Llama llamaEntity
                           && mc.player.getEyePosition().distanceTo(entityxxxxx.position()) < 10.0
                           && entityxxxxx.isAlive()) {
                           if (mc.player.getEyePosition().distanceTo(entityxxxxx.position()) < 5.0) {
                              this.llama = llamaEntity;
                              this.stage = Bot.Stage.Tame;
                           } else {
                              this.closeTo(entityxxxxx.blockPosition());
                           }

                           return;
                        }
                     }

                     if (mc.screen != null) {
                        mc.screen.onClose();
                     }

                     int slotxxx = InventoryUtil.findItemInventorySlot(Items.EGG);
                     InventoryUtil.inventorySwap(slotxxx, mc.player.getInventory().selected);
                     Alien.ROTATION.snapAt(Alien.ROTATION.getLastYaw(), 89.0F);
                     sendSequencedPacket(
                        id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                     );
                     InventoryUtil.inventorySwap(slotxxx, mc.player.getInventory().selected);
                     return;
                  }
                  case Tame: {
                     if (this.llama == null || this.llama.isDeadOrDying()) {
                        this.stage = Bot.Stage.Summon;
                        return;
                     }

                     int eggs = 0;
                     int hays = 0;

                     for (Entry<Integer, ItemStack> entryx : InventoryUtil.getInventoryAndHotbarSlots().entrySet()) {
                        if (entryx.getValue().getItem() == Items.EGG) {
                           eggs++;
                        }

                        if (entryx.getValue().getItem() == Blocks.HAY_BLOCK.asItem()) {
                           hays++;
                        }
                     }

                     if (eggs <= 1 || hays <= 1) {
                        this.closeScreen.reset();
                        this.stage = Bot.Stage.Open;
                        return;
                     }

                     if (mc.player.isPassenger()) {
                        if (this.llama.isTamed()) {
                           if (this.llama.hasChest()) {
                              int moves = 0;
                              if (mc.player.containerMenu instanceof HorseInventoryMenu shulkerx) {
                                 if (this.putTimer.passed(250L)) {
                                    if (!this.putIn) {
                                       for (Slot slotxxxxx : shulkerx.slots) {
                                          if (slotxxxxx.getItem().getItem() instanceof BlockItem blockItemx && blockItemx.getBlock() instanceof ShulkerBoxBlock
                                             )
                                           {
                                             mc.gameMode.handleInventoryMouseClick(shulkerx.containerId, slotxxxxx.index, 0, ClickType.QUICK_MOVE, mc.player);
                                             moves++;
                                             this.putTimer.reset();
                                             if (moves >= 15) {
                                                break;
                                             }
                                          }
                                       }

                                       this.putIn = true;
                                    } else {
                                       this.stage = Bot.Stage.Kill;
                                       mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, true)));
                                    }

                                    return;
                                 }
                              } else {
                                 this.putIn = false;
                                 this.putTimer.reset();
                                 mc.player.sendOpenInventory();
                              }

                              return;
                           } else {
                              mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, true)));
                              return;
                           }
                        }

                        return;
                     } else {
                        if (mc.player.getEyePosition().distanceTo(this.llama.position()) > 5.0) {
                           this.closeTo(this.llama.blockPosition());
                           return;
                        }

                        if (this.llama.isBaby()) {
                           if (mc.screen != null) {
                              mc.screen.onClose();
                           }

                           int slotxxxxxx = InventoryUtil.findBlockInventorySlot(Blocks.HAY_BLOCK);
                           InventoryUtil.inventorySwap(slotxxxxxx, mc.player.getInventory().selected);
                           Alien.ROTATION.lookAt(this.llama.position());
                           mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                           InventoryUtil.inventorySwap(slotxxxxxx, mc.player.getInventory().selected);
                        } else if (this.llama.isTamed()) {
                           if (this.llama.hasChest()) {
                              for (int i = 0; i < 9; i++) {
                                 if (mc.player.getInventory().getItem(i).isEmpty()) {
                                    InventoryUtil.switchToSlot(i);
                                    mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                                    return;
                                 }
                              }

                              for (int ix = 0; ix < 9; ix++) {
                                 if (mc.player.getInventory().getItem(ix).getItem() instanceof BlockItem blockItemxx
                                       && blockItemxx.getBlock() instanceof ShulkerBoxBlock
                                    || mc.player.getInventory().getItem(ix).getItem() == Items.LEATHER) {
                                    InventoryUtil.switchToSlot(ix);
                                    mc.player.drop(true);
                                    mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                                    return;
                                 }
                              }

                              return;
                           } else {
                              this.putTimer.reset();
                              this.putIn = false;
                              if (mc.screen != null) {
                                 mc.screen.onClose();
                              }

                              InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selected);
                              Alien.ROTATION.lookAt(this.llama.position());
                              mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                              InventoryUtil.inventorySwap(chestSlot, mc.player.getInventory().selected);
                           }
                        } else {
                           if (mc.screen != null) {
                              mc.screen.onClose();
                           }

                           for (int ixx = 0; ixx < 9; ixx++) {
                              if (mc.player.getInventory().getItem(ixx).isEmpty()) {
                                 InventoryUtil.switchToSlot(ixx);
                                 mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                                 return;
                              }
                           }

                           for (int ixxx = 0; ixxx < 9; ixxx++) {
                              if (mc.player.getInventory().getItem(ixxx).getItem() instanceof BlockItem blockItemxx
                                 && blockItemxx.getBlock() instanceof ShulkerBoxBlock) {
                                 InventoryUtil.switchToSlot(ixxx);
                                 mc.player.drop(true);
                                 mc.gameMode.interact(mc.player, this.llama, InteractionHand.MAIN_HAND);
                                 return;
                              }
                           }

                           return;
                        }
                     }

                     return;
                  }
                  case Kill: {
                     if (this.llama == null || this.llama.isDeadOrDying()) {
                        this.llama = null;
                        this.stage = Bot.Stage.Summon;
                        return;
                     }

                     if (mc.screen != null) {
                        mc.screen.onClose();
                     }

                     if (mc.player.isPassenger()) {
                        mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, true)));
                        return;
                     }

                     if (mc.player.position().distanceTo(this.llama.position()) > 1.0) {
                        this.closeTo(this.llama.blockPosition());
                     }

                     if (mc.player.position().distanceTo(this.llama.position()) > 2.0) {
                        return;
                     }

                     InventoryUtil.switchToSlot(swordSlot);
                     if (this.check()) {
                        Alien.ROTATION.lookAt(this.llama.getEyePosition());
                        mc.gameMode.attack(mc.player, this.llama);
                        EntityUtil.swingHand(InteractionHand.MAIN_HAND, SwingSide.All);
                     }

                     return;
                  }
                  default: {
                     return;
                  }
               }
            }
            case NPlusOneDupe: {
               BlockPos placePos = PacketMine.getBreakPos();
               if (placePos != null && BlockUtil.canPlace(placePos)) {
                  this.placeBlock(placePos);
               }
               break;
            }
            case TridentDupe: {
               long currentTime = System.currentTimeMillis();
               Iterator<Tuple<Long, Runnable>> iterator = this.scheduledTasks.iterator();

               while (iterator.hasNext()) {
                  Tuple<Long, Runnable> entry = iterator.next();
                  if (entry.getA() <= currentTime) {
                     entry.getB().run();
                     iterator.remove();
                  }
               }

               iterator = this.scheduledTasks2.iterator();

               while (iterator.hasNext()) {
                  Tuple<Long, Runnable> entry = iterator.next();
                  if (entry.getA() <= currentTime) {
                     entry.getB().run();
                     iterator.remove();
                  }
               }
               break;
            }
            case TurtlePath: {
               int seagrass = InventoryUtil.findItem(Items.SEAGRASS);
               if (seagrass == -1) {
                  if (BaritoneUtil.isActive()) {
                     BaritoneUtil.cancelEverything();
                  }

                  return;
               }

               if (this.timeOut.passedS(300.0)) {
                  this.inLove.clear();
                  this.timeOut.reset();
               }

               double distance = 0.0;
               Turtle target = null;

               for (Entity entity : Alien.THREAD.getEntities()) {
                  if (entity instanceof Turtle turtle
                     && !turtle.isBaby()
                     && !this.inLove.contains(turtle)
                     && !(Math.abs(mc.player.getY() - turtle.getY()) > 3.0)) {
                     double dis = mc.player.distanceTo(turtle);
                     if (target == null || dis < distance) {
                        distance = dis;
                        target = turtle;
                     }
                  }
               }

               if (target == null) {
                  if (this.timeOut.passedS(20.0)) {
                     this.inLove.clear();
                     this.timeOut.reset();
                  }

                  if (BaritoneUtil.isActive()) {
                     BaritoneUtil.cancelEverything();
                  }

                  return;
               }

               if (mc.player.distanceTo(target) < 3.0F) {
                  BaritoneUtil.cancelEverything();
                  InventoryUtil.switchToSlot(seagrass);
                  Alien.ROTATION.snapAt(target.position());
                  mc.getConnection().send(ServerboundInteractPacket.createInteractionPacket(target, mc.player.isShiftKeyDown(), InteractionHand.MAIN_HAND));
                  EntityUtil.swingHand(InteractionHand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
                  Alien.ROTATION.snapBack();
                  this.inLove.add(target);
               } else {
                  BaritoneUtil.gotoPos(target.blockPosition());
               }
               break;
            }
            case Turtle: {
               int seagrassx = InventoryUtil.findItem(Items.SEAGRASS);
               if (seagrassx == -1) {
                  return;
               }

               if (this.timeOut.passedS(300.0)) {
                  this.inLove.clear();
                  this.timeOut.reset();
               }

               double distance = 0.0;
               Turtle target = null;

               for (Entity entityx : Alien.THREAD.getEntities()) {
                  if (entityx instanceof Turtle turtlex && !turtlex.isBaby() && !this.inLove.contains(turtlex) && !(mc.player.distanceTo(turtlex) > 3.0F)) {
                     double dis = mc.player.distanceTo(turtlex);
                     if (target == null || dis < distance) {
                        distance = dis;
                        target = turtlex;
                     }
                  }
               }

               if (target == null) {
                  if (this.timeOut.passedS(20.0)) {
                     this.inLove.clear();
                     this.timeOut.reset();
                  }

                  return;
               }

               InventoryUtil.switchToSlot(seagrassx);
               Alien.ROTATION.snapAt(target.position());
               mc.getConnection().send(ServerboundInteractPacket.createInteractionPacket(target, mc.player.isShiftKeyDown(), InteractionHand.MAIN_HAND));
               EntityUtil.swingHand(InteractionHand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
               Alien.ROTATION.snapBack();
               this.inLove.add(target);
               break;
            }
            case SandMiner: {
               Block sandBlock = Blocks.SAND;
               if (this.redSand.getValue()) {
                  sandBlock = Blocks.RED_SAND;
               }

               if (Aura.INSTANCE.isOn() && Aura.INSTANCE.getTarget(Aura.INSTANCE.range.getValue()) != null) {
                  if (mc.screen != null) {
                     mc.screen.onClose();
                  }

                  int slot = findSwordSlot();
                  if (mc.player.getInventory().selected != slot) {
                     InventoryUtil.switchToSlot(slot);
                  }

                  BaritoneUtil.cancelEverything();
                  return;
               }

               if (mc.screen == null) {
                  this.screenTimeout.reset();
               } else if (this.screenTimeout.passedS(5.0)) {
                  mc.screen.onClose();
               }

               if (InventoryUtil.findClassInventorySlot(ShovelItem.class) == -1) {
                  BaritoneUtil.cancelEverything();
                  if (mc.player.containerMenu instanceof CraftingMenu craftingHandler) {
                     if (this.craftTimer.passedS(1.0)) {
                        int maxPerTick = 2;
                        int crafted = 0;

                        for (RecipeCollection recipeResult : mc.player.getRecipeBook().getCollections()) {
                           for (RecipeDisplayEntry recipe : recipeResult.getRecipes()) {
                              RecipeDisplay display = recipe.display();
                              if (display.result() instanceof ItemStackSlotDisplay stackSlotDisplay) {
                                 ItemStack previewStack = stackSlotDisplay.stack();
                                 if (previewStack.getItem() instanceof ShovelItem) {
                                    while (crafted < maxPerTick && BedCrafter.getEmptySlots() > 0) {
                                       mc.gameMode.handlePlaceRecipe(craftingHandler.containerId, recipe.id(), false);
                                       mc.gameMode.handleInventoryMouseClick(craftingHandler.containerId, 0, 1, ClickType.QUICK_MOVE, mc.player);
                                       crafted++;
                                       this.craftTimer.reset();
                                    }

                                    if (crafted >= maxPerTick) {
                                       return;
                                    }
                                 }
                              }
                           }
                        }

                        mc.player.closeContainer();
                     }
                  } else {
                     BlockPos bestPos = null;
                     double distance = 100.0;

                     for (BlockPos pos : BlockUtil.getSphere(3.0F)) {
                        if (mc.level.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE && BlockUtil.getClickSideStrict(pos) != null) {
                           BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
                           return;
                        }

                        if (BlockUtil.canPlace(pos)) {
                           double dist = mc.player.distanceToSqr(pos.getCenter());
                           if (bestPos == null || Math.sqrt(dist) < distance) {
                              bestPos = pos;
                              distance = Math.sqrt(dist);
                           }
                        }
                     }

                     if (bestPos != null) {
                        int craftTable = InventoryUtil.findItemInventorySlot(Items.CRAFTING_TABLE);
                        if (craftTable == -1) {
                           return;
                        }

                        InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selected);
                        BlockUtil.placeBlock(bestPos, this.rotate.getValue());
                        InventoryUtil.inventorySwap(craftTable, mc.player.getInventory().selected);
                     }
                  }

                  return;
               }

               Entity hasShulkerItemEntity = null;

               for (Entity entityxx : Alien.THREAD.getEntities()) {
                  if (entityxx instanceof ItemEntity itemEntity
                     && itemEntity.getItem().getItem() instanceof BlockItem item
                     && (item.getBlock() instanceof ShulkerBoxBlock || item.getBlock() == Blocks.ENDER_CHEST)) {
                     hasShulkerItemEntity = itemEntity;
                     break;
                  }
               }

               if (mc.player.containerMenu instanceof CraftingMenu && mc.screen != null) {
                  mc.screen.onClose();
               }

               int sands = InventoryUtil.getItemCount(Items.SAND);
               if (sands >= 1728) {
                  for (int slot1 = 9; slot1 < 36; slot1++) {
                     ItemStack stack = mc.player.getInventory().getItem(slot1);
                     if (!stack.isEmpty()
                        && stack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() == sandBlock
                        && stack.getCount() < stack.getMaxStackSize()) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot1, 1, ClickType.THROW, mc.player);
                     }
                  }

                  if (mc.screen instanceof InventoryScreen) {
                     mc.screen.onClose();
                     return;
                  }

                  int shulkerSlot = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                  BlockPos shulker = BlockUtil.getBlock(ShulkerBoxBlock.class, 3.0F);
                  if (mc.screen instanceof AbstractContainerScreen) {
                     AbstractContainerMenu var106 = mc.player.containerMenu;
                     if (var106 instanceof AbstractContainerMenu) {
                        if (!(mc.player.containerMenu instanceof ShulkerBoxMenu)) {
                           BaritoneUtil.cancelEverything();
                           if (shulkerSlot != -1) {
                              mc.screen.onClose();
                           } else {
                              Iterator var108 = var106.slots.iterator();

                              Slot slot;
                              do {
                                 if (!var108.hasNext()) {
                                    return;
                                 }

                                 slot = (Slot)var108.next();
                              } while (
                                 slot.index >= 27
                                    || !(
                                       slot.getItem().getItem() instanceof BlockItem blockItem
                                          && blockItem.getBlock() instanceof ShulkerBoxBlock
                                          && !ShulkerViewer.hasItems(slot.getItem())
                                    )
                              );

                              mc.gameMode.handleInventoryMouseClick(var106.containerId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
                           }
                        } else {
                           BaritoneUtil.cancelEverything();
                           this.storageSand = true;
                           if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerBoxScreenHandler) {
                              for (Slot slot : shulkerBoxScreenHandler.slots) {
                                 if (slot.index < 27 && slot.getItem().isEmpty()) {
                                    for (Slot slot2 : shulkerBoxScreenHandler.slots) {
                                       if (slot2.index >= 27
                                          && slot2.getItem().getItem() == Items.SAND
                                          && slot2.getItem().getCount() == slot2.getItem().getMaxStackSize()) {
                                          mc.gameMode
                                             .handleInventoryMouseClick(shulkerBoxScreenHandler.containerId, slot2.index, 0, ClickType.QUICK_MOVE, mc.player);
                                          return;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (shulker == null) {
                     double distance = 100.0;
                     BlockPos bestPos = null;

                     for (BlockPos pos : BlockUtil.getSphere(3.0F, mc.player.getEyePosition())) {
                        if (mc.level.isEmptyBlock(pos.above())
                           && BlockUtil.clientCanPlace(pos, false)
                           && BlockUtil.isStrictDirection(pos.relative(Direction.DOWN), Direction.UP)
                           && BlockUtil.canClick(pos.relative(Direction.DOWN))
                           && (bestPos == null || Mth.sqrt((float)mc.player.distanceToSqr(pos.getCenter())) < distance)) {
                           distance = Mth.sqrt((float)mc.player.distanceToSqr(pos.getCenter()));
                           bestPos = pos;
                        }
                     }

                     if (bestPos != null) {
                        BaritoneUtil.cancelEverything();
                        if (shulkerSlot != -1) {
                           InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selected);
                           BlockUtil.clickBlock(bestPos.relative(Direction.DOWN), Direction.UP, this.rotate.getValue());
                           InventoryUtil.inventorySwap(shulkerSlot, mc.player.getInventory().selected);
                        } else {
                           BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3.0F);
                           if (ec != null) {
                              BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), this.rotate.getValue());
                           } else {
                              int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                              if (enderChest != -1) {
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selected);
                                 BlockUtil.placeBlock(bestPos, true);
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selected);
                              }
                           }
                        }
                     }
                  } else {
                     BlockUtil.clickBlock(shulker, BlockUtil.getClickSide(shulker), this.rotate.getValue());
                  }
               } else if (this.storageSand) {
                  if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerBoxScreenHandler) {
                     for (Slot slotx : shulkerBoxScreenHandler.slots) {
                        if (slotx.index < 27 && slotx.getItem().isEmpty()) {
                           for (Slot slot2x : shulkerBoxScreenHandler.slots) {
                              if (slot2x.index >= 27
                                 && slot2x.getItem().getItem() == Items.SAND
                                 && slot2x.getItem().getCount() == slot2x.getItem().getMaxStackSize()) {
                                 mc.gameMode.handleInventoryMouseClick(shulkerBoxScreenHandler.containerId, slot2x.index, 0, ClickType.QUICK_MOVE, mc.player);
                                 return;
                              }
                           }
                        }
                     }

                     mc.screen.onClose();
                     this.storageSand = false;
                  }
               } else {
                  BlockPos shulkerx = BlockUtil.getBlock(ShulkerBoxBlock.class, 3.0F);
                  if (shulkerx != null) {
                     BaritoneUtil.mine(mc.level.getBlockState(shulkerx).getBlock());
                  } else {
                     int fillShulker = InventoryUtil.findClassInventorySlot(ShulkerBoxBlock.class);
                     if (fillShulker != -1) {
                        BaritoneUtil.cancelEverything();
                        AbstractContainerMenu var112 = mc.player.containerMenu;
                        if (var112 instanceof AbstractContainerMenu) {
                           for (Slot slotxx : var112.slots) {
                              if (slotxx.index >= 27
                                 && slotxx.getItem().getItem() instanceof BlockItem blockItem
                                 && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                                 mc.gameMode.handleInventoryMouseClick(var112.containerId, slotxx.index, 0, ClickType.QUICK_MOVE, mc.player);
                                 return;
                              }
                           }
                        }

                        double distance = 100.0;
                        BlockPos bestPos = null;

                        for (BlockPos posx : BlockUtil.getSphere(3.0F)) {
                           if (mc.level.isEmptyBlock(posx.above())
                              && BlockUtil.clientCanPlace(posx, false)
                              && BlockUtil.isStrictDirection(posx.relative(Direction.DOWN), Direction.UP)
                              && BlockUtil.canClick(posx.relative(Direction.DOWN))
                              && (bestPos == null || Mth.sqrt((float)mc.player.distanceToSqr(posx.getCenter())) < distance)) {
                              distance = Mth.sqrt((float)mc.player.distanceToSqr(posx.getCenter()));
                              bestPos = posx;
                           }
                        }

                        if (bestPos != null) {
                           BlockPos ec = BlockUtil.getBlock(Blocks.ENDER_CHEST, 3.0F);
                           if (ec != null) {
                              BlockUtil.clickBlock(ec, BlockUtil.getClickSide(ec), this.rotate.getValue());
                           } else {
                              int enderChest = InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                              if (enderChest != -1) {
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selected);
                                 BlockUtil.placeBlock(bestPos, true);
                                 InventoryUtil.inventorySwap(enderChest, mc.player.getInventory().selected);
                              }
                           }
                        }

                        return;
                     }

                     if (hasShulkerItemEntity != null) {
                        BaritoneUtil.gotoPos(hasShulkerItemEntity.blockPosition());
                     }

                     BlockPos posxx = BlockUtil.getBlock(Blocks.ENDER_CHEST, 5.0F);
                     if (posxx != null) {
                        BaritoneUtil.mine(Blocks.ENDER_CHEST);
                        if (mc.screen != null) {
                           mc.screen.onClose();
                        }

                        return;
                     }

                     if (this.ai.getValue()) {
                        BaritoneUtil.mine(sandBlock);
                     }

                     if (this.nuker.getValue()) {
                        if (!mc.player.onGround()) {
                           return;
                        }

                        int b = 0;

                        for (BlockPos sand : BlockUtil.getSphere(3.0F, mc.player.getEyePosition())) {
                           if (sandBlock == mc.level.getBlockState(sand).getBlock()) {
                              Direction side = BlockUtil.getClickSideStrict(sand);
                              if (side != null) {
                                 PlaceRender.INSTANCE.create(sand);
                                 Alien.ROTATION.snapAt(sand.getCenter());
                                 sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                                 sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, sand, side, id));
                                 sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, sand, side, id));
                                 Alien.ROTATION.snapBack();
                                 if (++b >= this.breaks.getValue()) {
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
               break;
            }
            case ScoreFarmer: {
               if (this.getScore.getValue() && this.commandTimer.passedS(4.0)) {
                  mc.player.connection.sendCommand("duel " + this.name.getValue());
                  this.commandTimer.reset();
               }

               if (mc.player.containerMenu instanceof ChestMenu && this.duelItemTimer.passedS(1.0)) {
                  mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 0, 0, ClickType.PICKUP, mc.player);
                  this.duelItemTimer.reset();
               }
               break;
            }
            case SlabPlacer: {
               if (this.inventory.getValue() && !EntityUtil.inInventory()) {
                  return;
               }

               this.placeProgress = 0;
               if (!this.slabPlacerDelay.passed((long)this.placeDelay.getValue())) {
                  return;
               }

               if (mc.player.isUsingItem() && this.usingPause.getValue()) {
                  return;
               }

               for (BlockPos posxxx : BlockUtil.getSphere(this.range.getValueFloat(), mc.player.position())) {
                  if ((
                        mc.level.getBlockState(posxxx).isCollisionShapeFullBlock(mc.level, posxxx)
                           || !(mc.level.getBlockState(posxxx).getBlock() instanceof SlabBlock)
                     )
                     && !mc.level.isEmptyBlock(posxxx)
                     && BlockUtil.canReplace(posxxx.above())) {
                     this.tryPlaceBlock(posxxx.above());
                  }
               }
               break;
            }
            case Ominous: {
               int ominousSlot = InventoryUtil.findItem(Items.OMINOUS_BOTTLE);
               if (ominousSlot != -1) {
                  if (!mc.player.hasEffect(MobEffects.BAD_OMEN) && !mc.player.hasEffect(MobEffects.RAID_OMEN)) {
                     if (this.ominousTimer.passedS(this.maxTime.getValue())) {
                        if (mc.player.getMainHandItem().getItem() != Items.OMINOUS_BOTTLE) {
                           this.lastSlot = mc.player.getInventory().selected;
                           InventoryUtil.switchToSlot(ominousSlot);
                        } else {
                           mc.options.keyUse.setDown(true);
                           this.ominousTimer.reset();
                        }
                     }
                  } else if (mc.player.getMainHandItem().getItem() == Items.OMINOUS_BOTTLE) {
                     mc.options.keyUse.setDown(false);
                     if (this.lastSlot != -1) {
                        InventoryUtil.switchToSlot(this.lastSlot);
                        this.lastSlot = -1;
                     }
                  }
               }
            }
         }
      } else {
         int food = InventoryUtil.findItem(Items.ENCHANTED_GOLDEN_APPLE);
         if (food == -1) {
            food = InventoryUtil.findItem(Items.GOLDEN_APPLE);
         }

         if (food == -1) {
            food = InventoryUtil.findItem(Items.GOLDEN_CARROT);
         }

         if (food == -1 && this.anyFood.getValue()) {
            food = InventoryUtil.getFood();
         }

         if (food != -1) {
            if (mc.screen != null && this.mode.is(Bot.Mode.XinDupe)) {
               mc.screen.onClose();
            }

            if (mc.player.getInventory().selected != food) {
               InventoryUtil.switchToSlot(food);
            }

            mc.options.keyUse.setDown(true);
            this.endEat = true;
            sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
         }

         BaritoneUtil.cancelEverything();
      }
   }

   @EventListener
   private void PacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.is(Bot.Mode.ScoreFarmer) && event.getPacket() instanceof ClientboundSystemChatPacket packet && packet.content() != null) {
            String received = packet.content().getString().replaceAll("§[a-zA-Z0-9]", "");
            if (!this.getScore.getValue() && received.contains("你收到一个决斗申请")) {
               mc.player.connection.sendCommand("duel accept " + this.name.getValue());
            } else if (!received.contains("<")) {
               if (this.getScore.getValue() && received.contains("Starting in 3 seconds")) {
                  mc.player.connection.sendCommand("suicide");
               } else if (!this.getScore.getValue() && received.contains("Starting in 4 seconds")) {
                  mc.player.connection.sendCommand("suicide");
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(UpdateRotateEvent event) {
      if (this.mode.is(Bot.Mode.XinDupe)) {
         event.setPitch(88.0F);
      }
   }

   private boolean check() {
      int at = ((ILivingEntity)mc.player).getLastAttackedTicks();
      return Math.max(at / Aura.getAttackCooldownProgressPerTick(), 0.0F) >= 1.3;
   }

   private void closeTo(BlockPos pos) {
      double speed = 0.19153333333333333;
      float forward = 1.0F;
      float side = 0.0F;
      float yaw = RotationManager.getRotation(pos.getCenter())[0];
      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double posX = forward * speed * cos + side * speed * sin;
      double posZ = forward * speed * sin - side * speed * cos;
      MovementUtil.setMotionX(posX);
      MovementUtil.setMotionZ(posZ);
   }

   public static int findSwordSlot() {
      if (mc.player == null) {
         return -1;
      } else {
         Inventory inv = mc.player.getInventory();

         for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.SWORDS)) {
               return i;
            }
         }

         for (int ix = 9; ix < inv.getContainerSize(); ix++) {
            ItemStack stack = inv.getItem(ix);
            if (!stack.isEmpty() && stack.is(ItemTags.SWORDS)) {
               return ix;
            }
         }

         return -1;
      }
   }

   public static enum Mode {
      AutoTrade,
      ItemFrameDupe,
      XinDupe,
      NPlusOneDupe,
      TridentDupe,
      TurtlePath,
      Turtle,
      SandMiner,
      ScoreFarmer,
      SlabPlacer,
      Ominous,
      None;
   }

   public static enum Stage {
      Open,
      Take,
      Summon,
      Tame,
      Kill;
   }
}
