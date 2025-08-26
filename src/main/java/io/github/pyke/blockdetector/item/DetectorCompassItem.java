package io.github.pyke.blockdetector.item;

import io.github.pyke.blockdetector.BlockDetector;
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
    public DetectorCompassItem(Properties properties) { super(properties); }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (stack.hasCustomHoverName()) return stack.getHoverName();
        return Component.translatable("item.block-detector.detector_compass");
    }

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

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) {
            return; // 클라이언트 측에서만 동작하도록 함
        }

        // NBT에서 Lodestone 정보 가져오기
        CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.contains("LodestonePos") && nbt.contains("LodestoneDimension")) {
            BlockPos lodestonePos = new BlockPos(
                nbt.getInt("LodestonePos_x"),
                nbt.getInt("LodestonePos_y"),
                nbt.getInt("LodestonePos_z")
            );
            ResourceKey<Level> lodestoneDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("LodestoneDimension")));

            // Lodestone의 위치와 현재 엔티티의 위치를 기반으로 CompassItemPropertyFunction을 사용하여 각도 계산
            GlobalPos globalPos = GlobalPos.of(lodestoneDimension, lodestonePos);

            // 클라이언트 측에서 각도 업데이트
            // 이 로직은 렌더링에 직접 관여하므로 클라이언트 측에서 처리해야 합니다.
            // 예를 들어 CompassItemPropertyFunction의 getAngle 메서드를 사용하거나,
            // custom property를 등록하여 클라이언트에서 처리하게 할 수 있습니다.

            // CompassItemPropertyFunction을 사용한 예시 (단, 이 함수가 public이어야 함)
            // CompassItemPropertyFunction.getAngle(world, stack, entity, globalPos);

            // 보다 안정적인 방법은 Custom property function을 등록하는 것입니다.
        }
    }
}
