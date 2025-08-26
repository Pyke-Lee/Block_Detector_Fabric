package io.github.pyke.blockdetector.client.scan;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class ClientScanner {
    final ClientLevel level;
    final BlockPos origin;
    final List<Vec3i> offsets;
    final Block target;
    final int perTickBudget;
    int cursor = 0;
    boolean finished = false;
    BlockPos foundPos = null;

    public ClientScanner(ClientLevel level, BlockPos origin, List<Vec3i> offsets, Block target, int durationTicks) {
        this.level = level; this.origin = origin; this.offsets = offsets; this.target = target;
        int N = Math.max(1, offsets.size());
        this.perTickBudget = Math.max(1, (int) Math.ceil(N / (double) Math.max(1, durationTicks)));
    }

    public void tick() {
        int processed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (cursor < offsets.size() && processed < perTickBudget && !finished) {
            var off = offsets.get(cursor++);
            pos.set(origin.getX() + off.getX(), origin.getY() + off.getY(), origin.getZ() + off.getZ());
            if (!level.hasChunk(pos.getX(), pos.getY())) { processed++; continue; }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getBlock() == target) {
                foundPos = pos.immutable();
                finished = true;
                break;
            }

            processed++;
        }
        if (cursor >= offsets.size()) { finished = true; }
    }

    public int getCursor() { return cursor; }
    public int total() { return offsets.size(); }
    public boolean isFinished() { return finished; }
    public BlockPos getFoundPos() { return foundPos; }
}
