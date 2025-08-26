package io.github.pyke.blockdetector.client;

import io.github.pyke.blockdetector.BlockDetector;
import io.github.pyke.blockdetector.client.hud.HudRenderer;
import io.github.pyke.blockdetector.client.hud.HudState;
import io.github.pyke.blockdetector.client.scan.manager.ClientScannerManager;
import io.github.pyke.blockdetector.network.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

public class BlockDetectorClient implements ClientModInitializer {
    private static KeyMapping DBG_TOGGLE;
    public static volatile boolean ANGLE_PREDICATE_REGISTERED = false;
    public static final java.util.concurrent.atomic.AtomicInteger ANGLE_CALLS = new java.util.concurrent.atomic.AtomicInteger();
    public static volatile float LAST_ANGLE = -1f;

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
        Item detectorCompass = BuiltInRegistries.ITEM.get(new ResourceLocation(BlockDetector.MOD_ID, "detector_compass"));

        // 커스텀 나침반에 'angle' 프레디킷을 등록
        ItemProperties.register(detectorCompass, new ResourceLocation("minecraft:angle"), (stack, level, entity, seed) -> {
//            if (level == null || entity == null) return 0f;
            return (entity.tickCount % 1200) / 1200f;

//            CompoundTag tag = stack.getTag();
//            if (tag == null || !tag.contains("LodestonePos", 10)) return 0f;
//
//            // 차원 일치 필수
//            String dim = tag.getString("LodestoneDimension");
//            if (!level.dimension().location().toString().equals(dim)) return 0f;
//
//            BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("LodestonePos"));
//            double dx = (pos.getX() + 0.5) - entity.getX();
//            double dz = (pos.getZ() + 0.5) - entity.getZ();
//            double target = Math.atan2(dz, dx);
//            double yawRad = Math.toRadians(entity.getYRot());
//            double rot = 0.5 - (yawRad - target) / (2*Math.PI);
//            rot -= Math.floor(rot);
//            return (float) rot;
        });
    }
}
