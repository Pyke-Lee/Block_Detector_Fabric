package io.github.pyke.blockdetector.client.hud;

import blue.endless.jankson.annotation.Nullable;
import io.github.pyke.blockdetector.BlockDetector;
import io.github.pyke.blockdetector.config.ConfigAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HudState {
    public enum STATE { IDLE, SCANNING, DETECTED, TRACKING, NOT_FOUND }

    private static final ResourceLocation DETECTOR_ID = new ResourceLocation(BlockDetector.MOD_ID, "detector_compass");

    private static STATE state = STATE.IDLE;
    private static int total = 1, cursor = 0;
    private static int ticksLeft = 0;
    private static BlockPos target = null;

    public static STATE getState() { return state; }

    public static void beginScanning(int totalOffsets) {
        state = STATE.SCANNING;
        total = Math.max(1, totalOffsets);
        cursor = 0;
        target = null;
    }

    public static void updateProgress(int cur, int tot) {
        if (state != STATE.SCANNING) { return; }
        cursor = cur;
        total = Math.max(1, tot);
    }

    public static void onDetected(BlockPos pos) {
        target = pos;
        state = STATE.DETECTED;
        ticksLeft = ConfigAccess.CONFIG.holdHud().detectedTicks;
    }

    public static void onNotFound() {
        state = STATE.NOT_FOUND;
        ticksLeft = ConfigAccess.CONFIG.holdHud().notFoundTicks;
        target = null;
    }

    public static void tick(Minecraft mc) {
        if (!ConfigAccess.CONFIG.holdHud().enabled) { return; }

        // 필요시 NBT에서 target 복구(재접속/리로드 대비)
        if (target == null) {
            BlockPos fromNbt = targetFromHeldItem(mc);
            if (fromNbt != null) target = fromNbt;
        }

        switch (state) {
            case DETECTED -> {
                if (ticksLeft > 0) ticksLeft--;
                if (ticksLeft <= 0) {
                    if (target == null) target = targetFromHeldItem(mc);
                    state = (target != null) ? STATE.TRACKING : STATE.IDLE; // ★ 여기서 반드시 TRACKING 전이
                }
            }
            case NOT_FOUND -> {
                if (ticksLeft > 0) ticksLeft--;
                if (ticksLeft <= 0) state = STATE.IDLE;
            }
            case TRACKING -> {
                HudState.currentText(mc);
            }
            default -> { }
        }
    }

    public static String currentText(Minecraft mc) {
        var hud = ConfigAccess.CONFIG.holdHud();

        if (target == null) {
            BlockPos nbtTarget = targetFromHeldItem(mc);
            if (nbtTarget != null) {
                target = nbtTarget;
                if (state == STATE.IDLE) { state = STATE.TRACKING; }
            }
        }

        switch (state) {
            case SCANNING -> {
                int prog = (int) Math.floor(100.0 * cursor / Math.max(1, total));
                prog = Math.max(0, Math.min(100, prog));
                return hud.scanningTemplate.replace("{PROG}", Integer.toString(prog));
            }
            case TRACKING -> {
                if (null == mc.player || null == target) { return ""; }
                int dist = distanceXZ(mc.player, target);
                return hud.trackingTemplate.replace("{DIST}", Integer.toString(dist));
            }
            default -> { return ""; }
        }
    }

    public static boolean shouldRender(Minecraft mc) {
        if (!ConfigAccess.CONFIG.holdHud().enabled) { return false; }
        if (null == mc.player) { return false; }
        if (!isHoldingDetector(mc.player)) { return false; }
        if (state == STATE.IDLE) {
            BlockPos nbtTarget = (null != target) ? target : targetFromHeldItem(mc);
            return nbtTarget != null;
        }
        if (state == STATE.TRACKING && null == target) { return false; }

        return true;
    }

    private static boolean isHoldingDetector(Player p) {
        if (null == p) { return false; }

        var main = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem());
        var off  = BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem());

        return DETECTOR_ID.equals(main) || DETECTOR_ID.equals(off);
    }

    private static int distanceXZ(Player p, BlockPos pos) {
        double dx = (pos.getX() + 0.5) - p.getX();
        double dz = (pos.getZ() + 0.5) - p.getZ();
        return (int) Math.floor(Math.hypot(dx, dz));
    }

    @Nullable
    private static BlockPos targetFromHeldItem(Minecraft mc) {
        if (mc.player == null) return null;
        var levelDim = mc.player.level().dimension().location().toString();

        ItemStack[] hands = { mc.player.getMainHandItem(), mc.player.getOffhandItem() };
        for (ItemStack st : hands) {
            if (st.isEmpty()) continue;
            var key = BuiltInRegistries.ITEM.getKey(st.getItem());
            if (!DETECTOR_ID.equals(key)) continue;

            CompoundTag tag = st.getTag();
            if (tag == null || !tag.contains("LodestonePos", 10)) continue;

            String dim = tag.getString("LodestoneDimension");
            if (!levelDim.equals(dim)) return null;

            return NbtUtils.readBlockPos(tag.getCompound("LodestonePos"));
        }
        return null;
    }
}
