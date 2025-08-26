package io.github.pyke.blockdetector.client;

import io.github.pyke.blockdetector.client.hud.HudRenderer;
import io.github.pyke.blockdetector.client.hud.HudState;
import io.github.pyke.blockdetector.client.scan.manager.ClientScannerManager;
import io.github.pyke.blockdetector.item.DetectorCompass;
import io.github.pyke.blockdetector.item.DetectorCompassItem;
import io.github.pyke.blockdetector.network.Network;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        final Item item = DetectorCompass.get();

        ItemProperties.register(DetectorCompass.get(), new ResourceLocation("minecraft", "angle"), (ItemStack stack, ClientLevel level, LivingEntity entity, int seed) -> {
            // level 보강
                if (level == null) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null || mc.level == null) return 0.0f;
                    level = mc.level;
                }
                final LivingEntity base = (entity != null) ? entity : Minecraft.getInstance().player;
                if (base == null) return 0.0f;

                // ★ 무조건 Lodestone만 사용
                final GlobalPos target = DetectorCompassItem.getLodestoneFromStack(stack);
                if (target == null) return 0.0f;
                if (!level.dimension().equals(target.dimension())) return 0.0f;

                final BlockPos tp = target.pos();
                double dx = (tp.getX() + 0.5D) - base.getX();
                double dz = (tp.getZ() + 0.5D) - base.getZ();
                double toTarget = Math.atan2(dz, dx);
                double player   = Math.toRadians(base.getYRot());
                double rel = toTarget - player;
                while (rel < -Math.PI) rel += Math.PI * 2D;
                while (rel >  Math.PI) rel -= Math.PI * 2D;

                float result = (float)((rel / (Math.PI * 2D)) + 0.5D + 0.25D);

                return (float) (result - Math.floor(result)); // [0,1)
            }
        );
    }
}
