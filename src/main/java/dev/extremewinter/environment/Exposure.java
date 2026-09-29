package dev.extremewinter.environment;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public final class Exposure {
    private Exposure() { }

    /** Heightmap also recognizes glass roofs; sky light alone would not protect greenhouses. */
    public static boolean outdoors(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos)
                && world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }
}
