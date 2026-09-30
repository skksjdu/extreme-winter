package dev.extremewinter.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Sparse snow may pass foliage, but never a solid ceiling. All scans stay in a loaded column. */
public final class CanopySnow {
    public static final int INTERVAL_MULTIPLIER = 4;

    private CanopySnow() { }

    public static boolean belowLeaves(Level world, BlockPos pos) {
        var chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || !world.isInWorldBounds(pos)) return false;
        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX() & 15, pos.getZ() & 15) + 1;
        if (top <= pos.getY() + 1) return false;
        boolean leaves = false;
        var scan = pos.mutable();
        for (int y = pos.getY() + 1; y < top; y++) {
            var state = world.getBlockState(scan.setY(y));
            if (state.is(BlockTags.LEAVES)) leaves = true;
            else if (!state.isAir() && !(leaves && SnowDriftBlock.layers(state) > 0)) return false;
        }
        return leaves;
    }

    public static BlockPos floorBelowLeaves(Level world, int x, int z) {
        var chunk = world.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;
        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
        boolean leaves = false;
        var scan = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top - 1; y >= world.getMinY(); y--) {
            var state = world.getBlockState(scan.setY(y));
            if (state.is(BlockTags.LEAVES)) leaves = true;
            else if (!state.isAir()) {
                int snow = SnowDriftBlock.layers(state);
                if (snow > 0 && !leaves) continue; // Snow on the canopy must not hide the forest floor.
                return leaves ? (snow > 0 ? scan.immutable() : scan.above()) : null;
            }
        }
        return null;
    }
}
