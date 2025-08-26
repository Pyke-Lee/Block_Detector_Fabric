package io.github.pyke.blockdetector.client;

import io.github.pyke.blockdetector.client.hud.HudRenderer;
import io.github.pyke.blockdetector.client.hud.HudState;
import io.github.pyke.blockdetector.client.scan.manager.ClientScannerManager;
import io.github.pyke.blockdetector.item.DetectorCompass;
import io.github.pyke.blockdetector.network.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlockDetectorClient implements ClientModInitializer {
    private static final Wobble wobble = new Wobble();

    @Override
    public void onInitializeClient() {
        // S2C
        ClientPlayNetworking.registerGlobalReceiver(Network.REQ_SCAN, ClientScannerManager::onScanRequest);

        // HUD / Scanner Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientScannerManager.tick(client);
            HudState.tick(client);
        });

        // HUD Render
        HudRenderer.register();

        registerCompassAngle();
    }

    private static void registerCompassAngle() {
        Item detectorCompass = BuiltInRegistries.ITEM.get(DetectorCompass.DETECTOR_ID);

        ItemProperties.register(detectorCompass, new ResourceLocation("minecraft", "angle"),
            (stack, level, entity, seed) -> computeAngle(stack, level, entity)
        );
    }

    private static float computeAngle(ItemStack stack, ClientLevel level, LivingEntity entity) {
        if (entity == null) return 0f; // 인벤토리 프리뷰 등

        // 1) Lodestone 태그가 있으면 바닐라 규칙으로 처리
        var tag = stack.getTag();
        if (tag != null && tag.contains("LodestonePos", 10)) {
            // 차원 확인(다르면 스핀)
            if (level == null) return spin(entity.tickCount);
            String dim = tag.getString("LodestoneDimension");
            if (!level.dimension().location().toString().equals(dim)) {
                return spin(entity.tickCount);
            }
            BlockPos pos = net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("LodestonePos"));
            return angleTo(entity, level, pos, wobble);
        }

        // 2) 바닐라 기본 규칙: 오버월드면 스폰을 가리키고, 다른 차원은 스핀
        if (level == null) return 0f;
        if (level.dimension() != Level.OVERWORLD) {
            return spin(entity.tickCount); // 네더/엔드에서는 회전
        } else {
            BlockPos spawn = level.getSharedSpawnPos(); // 오버월드 스폰
            return angleTo(entity, level, spawn, wobble);
        }
    }

    private static float angleTo(Entity entity, Level level, BlockPos target, Wobble wb) {
        double dx = (target.getX() + 0.5) - entity.getX();
        double dz = (target.getZ() + 0.5) - entity.getZ();

        double targetRad = Math.atan2(dz, dx);
        double yawRad = Math.toRadians(entity.getYRot()); // 플레이어 시야(라디안)

        // 바닐라 규약: 0..1
        double raw = 0.5 - (yawRad - targetRad) / (2 * Math.PI);
        raw = Mth.positiveModulo(raw, 1.0);

        // 바닐라식 wobble(프레임간 보간/흔들림)
        wb.update(level.getGameTime(), raw);
        return (float) wb.rotation;
    }

    private static float spin(int tick) {
        return (tick % 1200) / 1200f; // 60초에 한 바퀴
    }

    private static final class Wobble {
        double rotation;       // 0..1
        double deltaRotation;  // 변화량
        long lastTick;

        void update(long gameTime, double target) {
            if (gameTime == lastTick) return;
            lastTick = gameTime;

            double d = target - rotation;
            d = Mth.positiveModulo(d + 0.5, 1.0) - 0.5; // -0.5..0.5

            deltaRotation += d * 0.1;
            deltaRotation *= 0.8;
            rotation = Mth.positiveModulo(rotation + deltaRotation, 1.0);
        }
    }
}
