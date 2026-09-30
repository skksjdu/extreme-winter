package dev.extremewinter.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Exposure {
    private Exposure() { }

    /** A roof must cover the center and at least seven columns of its 3x3 neighborhood.
     * Heightmaps include glass and foliage. Missing chunks count as exposed, never load them.
     * Callers supply the air above the heat source or the player's feet, excluding the source itself.
     */
    public static boolean outdoors(ServerLevel world, BlockPos pos) {
        if (!covered(world, pos)) return true;
        int roofs = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (covered(world, pos.offset(dx, 0, dz))) roofs++;
        }
        return roofs < 7;
    }

    private static boolean covered(ServerLevel world, BlockPos pos) {
        return world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY();
    }
}
