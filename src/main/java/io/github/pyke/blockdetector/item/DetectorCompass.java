package io.github.pyke.blockdetector.item;

import io.github.pyke.blockdetector.BlockDetector;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DetectorCompass {
    public static final ResourceLocation DETECTOR_ID = new ResourceLocation(BlockDetector.MOD_ID, "detector_compass");
    public static final Item DETECTOR = new DetectorCompassItem(new Item.Properties().stacksTo(1));

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, DETECTOR_ID, DETECTOR);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(DETECTOR));
    }

    public static void updateLodestoneForHeldDetector(ServerPlayer serverPlayer, BlockPos pos) {
        ItemStack item = getHeldDetector(serverPlayer);
        if (item == null) return;

        CompoundTag tag = item.getOrCreateTag();
        CompoundTag lodestonePosTag = new CompoundTag();
        lodestonePosTag.putInt("x", pos.getX());
        lodestonePosTag.putInt("y", pos.getY());
        lodestonePosTag.putInt("z", pos.getZ());
        tag.put("LodestonePos", lodestonePosTag);
//        tag.put("LodestonePos", NbtUtils.writeBlockPos(pos));
        tag.putString("LodestoneDimension", serverPlayer.level().dimension().location().toString());
        tag.putBoolean("LodestoneTracked", false);
    }

    public static ItemStack getHeldDetector(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == DETECTOR) { return main; }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() == DETECTOR) { return off; }

        return ItemStack.EMPTY;
    }
}
