package io.github.pyke.blockdetector.item;

import blue.endless.jankson.annotation.Nullable;
import io.github.pyke.blockdetector.config.ConfigAccess;
import io.github.pyke.blockdetector.network.Network;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DetectorCompassItem extends CompassItem {
    public static final String NBT_LODESTONE_POS = "LodestonePos";
    public static final String NBT_LODESTONE_DIM = "LodestoneDimension";
    public static final String NBT_LODESTONE_TRACKED = "LodestoneTracked";

    public DetectorCompassItem(Properties properties) { super(properties); }

    public static @Nullable GlobalPos getLodestoneFromStack(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return null;
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return null;

        if (!nbt.contains(NBT_LODESTONE_POS, Tag.TAG_COMPOUND)) return null;
        if (!nbt.contains(NBT_LODESTONE_DIM, Tag.TAG_STRING)) return null;

        CompoundTag lp = nbt.getCompound(NBT_LODESTONE_POS);
        BlockPos pos = new BlockPos(lp.getInt("x"), lp.getInt("y"), lp.getInt("z"));
        ResourceKey<Level> dim = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(nbt.getString(NBT_LODESTONE_DIM))
        );
        return GlobalPos.of(dim, pos);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (stack.hasCustomHoverName()) return stack.getHoverName();
        return Component.translatable("item.block-detector.detector_compass");
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack item = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(item);
        }

        // Client return true
        if (level.isClientSide()) { return InteractionResultHolder.sidedSuccess(item, true); }

        // Server Side
        final ServerPlayer serverPlayer = (ServerPlayer) player;
        final var config = ConfigAccess.CONFIG;

        final CompoundTag nbt = item.getTag();
        if (null == nbt || !nbt.contains("target", Tag.TAG_COMPOUND)) {
            serverPlayer.displayClientMessage(Component.literal(config.holdHud().nullTargetTemplate), true);
            return InteractionResultHolder.success(item);
        }
        final String targetID = nbt.getCompound("target").getString("type");
        if (null == targetID || targetID.isBlank()) {
            serverPlayer.displayClientMessage(Component.literal(config.holdHud().nullTargetTemplate), true);
            return InteractionResultHolder.success(item);
        }

        // 설정 정규화 (경계값 보정)
        ConfigAccess.normalize();

        // range/cooldown : 아이템 NBT 우선 → 없으면 설정값
        int range    = config.detectorRange();
        int cooldown = config.cooldownTicks();
        if (nbt.contains("detector", Tag.TAG_COMPOUND)) {
            final CompoundTag detectorTag = nbt.getCompound("detector");
            if (detectorTag.contains("range", Tag.TAG_INT))    { range    = detectorTag.getInt("range"); }
            if (detectorTag.contains("cooldown", Tag.TAG_INT)) { cooldown = detectorTag.getInt("cooldown"); }
        }

        final int step     = config.scanStep();
        final int duration = config.scanDurationTicks();

        // 쿨다운 부여
        if (cooldown > 0) { serverPlayer.getCooldowns().addCooldown(this, cooldown); }

        // S2C : 스캔 파라미터 전송
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(64));
        buf.writeBlockPos(serverPlayer.blockPosition());
        buf.writeVarInt(range);
        buf.writeVarInt(step);
        buf.writeVarInt(duration);
        buf.writeUtf(targetID);
        ServerPlayNetworking.send(serverPlayer, Network.REQ_SCAN, buf);

        return InteractionResultHolder.sidedSuccess(item, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) return;

        // 저장 형식: Compound "LodestonePos" {x,y,z}, String "LodestoneDimension", Boolean "LodestoneTracked"
        final CompoundTag nbt = stack.getTag();
        if (null == nbt) return;

        if (nbt.contains(NBT_LODESTONE_POS, Tag.TAG_COMPOUND) && nbt.contains(NBT_LODESTONE_DIM, Tag.TAG_STRING)) {
            final CompoundTag lp = nbt.getCompound(NBT_LODESTONE_POS);
            final BlockPos lodestonePos = new BlockPos(lp.getInt("x"), lp.getInt("y"), lp.getInt("z"));
            final ResourceKey<Level> lodestoneDim = ResourceKey.create(
                Registries.DIMENSION,
                new ResourceLocation(nbt.getString(NBT_LODESTONE_DIM))
            );
            final GlobalPos globalPos = GlobalPos.of(lodestoneDim, lodestonePos);
        }
    }
}
