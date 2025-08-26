package io.github.pyke.blockdetector.item;

import io.github.pyke.blockdetector.BlockDetector;
import io.github.pyke.blockdetector.config.ConfigAccess;
import io.github.pyke.blockdetector.network.Network;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DetectorCompassItem extends CompassItem {
    public DetectorCompassItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) { return InteractionResultHolder.pass(item); }

        if (!level.isClientSide()) {
            var serverPlayer = (ServerPlayer) player;
            var config = ConfigAccess.CONFIG;

            CompoundTag nbt = item.getTag();
            if (null == nbt || !nbt.contains("target", Tag.TAG_COMPOUND)) {
                serverPlayer.displayClientMessage(Component.literal(config.holdHud().nullTargetTemplate), true);
                return InteractionResultHolder.success(item);
            }
            String targetID = nbt.getCompound("target").getString("type");
            if (null == targetID || targetID.isBlank()) {
                serverPlayer.displayClientMessage(Component.literal(config.holdHud().nullTargetTemplate), true);
                return InteractionResultHolder.success(item);
            }

            // range/cooldown - Item NBT Override -> null Config Default
            int range = config.detectorRange();
            int cooldown = config.cooldownTicks();
            if (nbt.contains("detector", Tag.TAG_COMPOUND)) {
                CompoundTag detectorTag = nbt.getCompound("detector");
                if (detectorTag.contains("range", Tag.TAG_INT)) { range = detectorTag.getInt("range"); }
                if (detectorTag.contains("cooldown", Tag.TAG_INT)) { cooldown = detectorTag.getInt("cooldown"); }
            }

            int step = config.scanStep();
            int duration = config.scanDurationTicks();

            ConfigAccess.normalize();

            if (cooldown > 0) { serverPlayer.getCooldowns().addCooldown(this, cooldown); }

            // S2C
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBlockPos(serverPlayer.blockPosition());
            buf.writeVarInt(range);
            buf.writeVarInt(step);
            buf.writeVarInt(duration);
            buf.writeUtf(targetID);

            ServerPlayNetworking.send(serverPlayer, Network.REQ_SCAN, buf);
        }

        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }
}
