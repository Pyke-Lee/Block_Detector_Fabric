package io.github.pyke.blockdetector.client.scan.manager;

import io.github.pyke.blockdetector.client.hud.HudState;
import io.github.pyke.blockdetector.client.scan.ClientScanner;
import io.github.pyke.blockdetector.network.Network;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientScannerManager {
    private static ClientScanner active;

    // S2C: Scan Request
    public static void onScanRequest(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender sender) {
        BlockPos origin = buf.readBlockPos();
        int range = buf.readVarInt();
        int step = buf.readVarInt();
        int duration = buf.readVarInt();
        String targetID = buf.readUtf();

        client.execute(() -> {
            ClientLevel level = client.level;
            if (null == level) { return; }

            Block target = BuiltInRegistries.BLOCK.get(new ResourceLocation(targetID));
            if (null == target) { sendResult(false, 0, 0, 0); return; }

            List<Vec3i> offsets = buildOffsets(range, step);
            active = new ClientScanner(level, origin, offsets, target, duration);
            HudState.beginScanning(offsets.size());
        });
    }

    public static void tick(Minecraft client) {
        if (null == active) { return; }
        active.tick();
        HudState.updateProgress(active.getCursor(), active.total());

        if (active.isFinished()) {
            BlockPos foundPos = active.getFoundPos();

            if (null != foundPos) {
                sendResult(true, foundPos.getX(), foundPos.getY(), foundPos.getZ());
                HudState.onDetected(foundPos);
            }
            else {
                sendResult(false, 0, 0, 0);
                HudState.onNotFound();
            }

            active = null;
        }
    }

    private static void sendResult(boolean found, int x, int y, int z) {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        out.writeBoolean(found);
        if (found) { out.writeInt(x); out.writeInt(y); out.writeInt(z); }
        ClientPlayNetworking.send(Network.RES_SCAN, out);
    }

    private static List<Vec3i> buildOffsets(int r, int step) {
        // 1) 중복을 방지하면서 삽입 순서를 유지
        java.util.LinkedHashSet<Vec3i> set = new java.util.LinkedHashSet<>();

        // ✔ 내부 코어: 가까운 구체 반경은 무조건 step=1로 촘촘히 검사
        int dense = Math.min(r, Math.max(2, step)); // 최소 2칸, step보다 작지 않게
        int dense2 = dense * dense;
        for (int dx = -dense; dx <= dense; dx++) {
            for (int dy = -dense; dy <= dense; dy++) {
                for (int dz = -dense; dz <= dense; dz++) {
                    int d2 = dx*dx + dy*dy + dz*dz;
                    if (d2 <= dense2) set.add(new Vec3i(dx, dy, dz));
                }
            }
        }

        // ✔ 외부 셸: 그 밖의 영역은 step 간격으로 희소 샘플링
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx += Math.max(1, step)) {
            for (int dy = -r; dy <= r; dy += Math.max(1, step)) {
                for (int dz = -r; dz <= r; dz += Math.max(1, step)) {
                    int d2 = dx*dx + dy*dy + dz*dz;
                    if (d2 <= r2) set.add(new Vec3i(dx, dy, dz));
                }
            }
        }

        // ✔ 수평(XZ) 거리 오름차순 정렬(같은 거리면 먼저 들어온 순서 유지)
        java.util.List<Vec3i> list = new java.util.ArrayList<>(set);
        list.sort(java.util.Comparator.comparingInt(v -> v.getX()*v.getX() + v.getZ()*v.getZ()));
        return list;
    }
}
