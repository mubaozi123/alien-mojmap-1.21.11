package dev.luminous.mod.modules.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntitySpawnedEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.Render3DScheduler;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.util.Comparator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ESP extends Module {
   public static ESP INSTANCE;
   public final BooleanSetting box = this.add(new BooleanSetting("BoxESP", true).setParent());
   private final ColorSetting endPortalFill = this.add(new ColorSetting("EndPortalFill", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting endPortalOutline = this.add(
      new ColorSetting("EndPortalOutline", new Color(255, 243, 129, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting itemFill = this.add(new ColorSetting("ItemFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting itemOutline = this.add(new ColorSetting("ItemOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerFill = this.add(new ColorSetting("PlayerFill", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting playerOutline = this.add(new ColorSetting("PlayerOutline", new Color(255, 255, 255, 100), this.box::isOpen).injectBoolean(true));
   private final ColorSetting chestFill = this.add(new ColorSetting("ChestFill", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting chestOutline = this.add(new ColorSetting("ChestOutline", new Color(255, 198, 123, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting enderChestFill = this.add(
      new ColorSetting("EnderChestFill", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting enderChestOutline = this.add(
      new ColorSetting("EnderChestOutline", new Color(255, 100, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   private final ColorSetting shulkerBoxFill = this.add(new ColorSetting("ShulkerBoxFill", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false));
   private final ColorSetting shulkerBoxOutline = this.add(
      new ColorSetting("ShulkerBoxOutline", new Color(15, 255, 255, 100), this.box::isOpen).injectBoolean(false)
   );
   public final BooleanSetting item = this.add(new BooleanSetting("ItemName", false).setParent());
   public final BooleanSetting customName = this.add(new BooleanSetting("CustomName", false, this.item::isOpen));
   public final BooleanSetting count = this.add(new BooleanSetting("Count", true, this.item::isOpen));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255), this.item::isOpen));
   public final BooleanSetting pearl = this.add(new BooleanSetting("PearlOwner", true));

   public ESP() {
      super("ESP", Module.Category.Render);
      this.setChinese("透视");
      INSTANCE = this;
   }

   @Override
   public void onRender3D(PoseStack matrixStack) {
      if (this.item.getValue()) {
         for (Entity entity : Alien.THREAD.getEntities()) {
            if (entity instanceof ItemEntity itemEntity) {
               int itemCount = itemEntity.getItem().getCount();
               String s = this.count.getValue() && itemCount > 1 ? " x" + itemCount : "";
               String name = (this.customName.getValue() ? itemEntity.getItem().getHoverName() : itemEntity.getItem().getItem().getName()).getString();
               Render3DUtil.drawText3D(
                  name + s,
                  ((IEntity)itemEntity)
                     .getDimensions()
                     .makeBoundingBox(
                        new Vec3(
                           MathUtil.interpolate(itemEntity.xOld, itemEntity.getX(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                           MathUtil.interpolate(itemEntity.yOld, itemEntity.getY(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                           MathUtil.interpolate(itemEntity.zOld, itemEntity.getZ(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
                        )
                     )
                     .inflate(0.0, 0.1, 0.0)
                     .getCenter()
                     .add(0.0, 0.5, 0.0),
                  this.text.getValue()
               );
            }
         }
      }

      if (this.box.getValue()) {
         if (this.itemFill.booleanValue || this.itemOutline.booleanValue || this.playerFill.booleanValue || this.playerOutline.booleanValue) {
            for (Entity entityx : Alien.THREAD.getEntities()) {
               if (!(entityx instanceof ItemEntity) || !this.itemFill.booleanValue && !this.itemOutline.booleanValue) {
                  if (entityx instanceof Player && (this.playerFill.booleanValue || this.playerOutline.booleanValue)) {
                     Color color = this.playerFill.getValue();
                     Render3DScheduler.INSTANCE
                        .addBox(
                           ((IEntity)entityx)
                              .getDimensions()
                              .makeBoundingBox(
                                 new Vec3(
                                    MathUtil.interpolate(entityx.xOld, entityx.getX(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                                    MathUtil.interpolate(entityx.yOld, entityx.getY(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                                    MathUtil.interpolate(entityx.zOld, entityx.getZ(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
                                 )
                              )
                              .inflate(0.0, 0.1, 0.0),
                           this.playerFill.getValue(),
                           this.playerOutline.getValue(),
                           this.playerOutline.booleanValue,
                           this.playerFill.booleanValue
                        );
                  }
               } else {
                  Color color = this.itemFill.getValue();
                  Render3DScheduler.INSTANCE
                     .addBox(
                        ((IEntity)entityx)
                           .getDimensions()
                           .makeBoundingBox(
                              new Vec3(
                                 MathUtil.interpolate(entityx.xOld, entityx.getX(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                                 MathUtil.interpolate(entityx.yOld, entityx.getY(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
                                 MathUtil.interpolate(entityx.zOld, entityx.getZ(), (double)mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
                              )
                           ),
                        this.itemFill.getValue(),
                        this.itemOutline.getValue(),
                        this.itemOutline.booleanValue,
                        this.itemFill.booleanValue
                     );
               }
            }
         }

         for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
            if (!(blockEntity instanceof ChestBlockEntity) || !this.chestFill.booleanValue && !this.chestOutline.booleanValue) {
               if (!(blockEntity instanceof EnderChestBlockEntity) || !this.enderChestFill.booleanValue && !this.enderChestOutline.booleanValue) {
                  if (!(blockEntity instanceof ShulkerBoxBlockEntity) || !this.shulkerBoxFill.booleanValue && !this.shulkerBoxOutline.booleanValue) {
                     if (blockEntity instanceof TheEndPortalBlockEntity && (this.endPortalFill.booleanValue || this.endPortalOutline.booleanValue)) {
                        AABB box = new AABB(blockEntity.getBlockPos());
                        Render3DUtil.draw3DBox(
                           matrixStack,
                           box,
                           this.endPortalFill.getValue(),
                           this.endPortalOutline.getValue(),
                           this.endPortalOutline.booleanValue,
                           this.endPortalFill.booleanValue
                        );
                     }
                  } else {
                     AABB box = new AABB(blockEntity.getBlockPos());
                     Render3DUtil.draw3DBox(
                        matrixStack,
                        box,
                        this.shulkerBoxFill.getValue(),
                        this.shulkerBoxOutline.getValue(),
                        this.shulkerBoxOutline.booleanValue,
                        this.shulkerBoxFill.booleanValue
                     );
                  }
               } else {
                  AABB box = new AABB(blockEntity.getBlockPos());
                  Render3DUtil.draw3DBox(
                     matrixStack,
                     box,
                     this.enderChestFill.getValue(),
                     this.enderChestOutline.getValue(),
                     this.enderChestOutline.booleanValue,
                     this.enderChestFill.booleanValue
                  );
               }
            } else {
               AABB box = new AABB(blockEntity.getBlockPos());
               Render3DUtil.draw3DBox(
                  matrixStack, box, this.chestFill.getValue(), this.chestOutline.getValue(), this.chestOutline.booleanValue, this.chestFill.booleanValue
               );
            }
         }
      }
   }

   @EventListener
   public void onReceivePacket(EntitySpawnedEvent event) {
      if (!nullCheck()) {
         if (this.pearl.getValue() && event.getEntity() instanceof ThrownEnderpearl pearlEntity) {
            if (pearlEntity.getOwner() != null) {
               pearlEntity.setCustomName(pearlEntity.getOwner().getName());
               pearlEntity.setCustomNameVisible(true);
            } else {
               mc.level
                  .players()
                  .stream()
                  .min(Comparator.comparingDouble(p -> p.position().distanceTo(new Vec3(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()))))
                  .ifPresent(player -> {
                     pearlEntity.setCustomName(player.getName());
                     pearlEntity.setCustomNameVisible(true);
                  });
            }
         }
      }
   }
}
