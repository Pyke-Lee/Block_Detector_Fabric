package io.github.pyke.blockdetector.client;

import io.github.pyke.blockdetector.client.hud.HudRenderer;
import io.github.pyke.blockdetector.client.hud.HudState;
import io.github.pyke.blockdetector.client.scan.manager.ClientScannerManager;
import io.github.pyke.blockdetector.item.DetectorCompass;
import io.github.pyke.blockdetector.network.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BlockDetectorClient implements ClientModInitializer {
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

        ItemProperties.register(detectorCompass, new ResourceLocation("angle"), new CompassItemPropertyFunction((world, stack, entity) -> {
            CompoundTag nbt = stack.getTag();
            if (nbt != null && nbt.contains("LodestonePos") && nbt.contains("LodestoneDimension")) {
                // 저장된 LodestonePos 정보를 읽습니다.
                CompoundTag posTag = nbt.getCompound("LodestonePos");
                BlockPos lodestonePos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));

                // 저장된 LodestoneDimension 정보를 읽습니다.
                ResourceKey<Level> lodestoneDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("LodestoneDimension")));

                // BlockPos와 ResourceKey<Level>을 사용하여 GlobalPos를 생성하고 반환합니다.
                return GlobalPos.of(lodestoneDimension, lodestonePos);
            }

            // Lodestone 정보가 없으면 null을 반환합니다.
            return null;
        }));
    }
}
