package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Bounded scan once per player per second, nearest first, never loads chunks. */
public final class HeatSources {
    private final int radius;
    private final List<BlockPos> offsets;

    public HeatSources(int radius) {
        this.radius = radius;
        var positions = new ArrayList<BlockPos>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) positions.add(new BlockPos(x, y, z));
                }
            }
        }
        positions.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(BlockPos.ORIGIN)));
        offsets = List.copyOf(positions);
    }

    public double strength(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos origin = player.getBlockPos();
        double strongest = 0;
        for (BlockPos offset : offsets) {
            BlockPos pos = origin.add(offset);
            if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            if (!isHeatSource(world.getBlockState(pos))) continue;
            double distance = player.getPos().distanceTo(Vec3d.ofCenter(pos));
            if (distance > radius) continue;
            // Check the small ray corridor before raycasting, so a chunk border cannot cause a load.
            if (!world.isRegionLoaded(Math.min(pos.getX(), origin.getX()), Math.min(pos.getZ(), origin.getZ()),
                    Math.max(pos.getX(), origin.getX()), Math.max(pos.getZ(), origin.getZ()))) continue;
            var hit = world.raycast(new RaycastContext(player.getEyePos(), Vec3d.ofCenter(pos),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                strongest = Math.max(strongest, 1 - 0.75 * distance / radius);
            }
        }
        return strongest;
    }

    public static boolean isHeatSource(BlockState state) {
        if (state.isOf(Blocks.LAVA)) return true;
        return (state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE)
                || state.isOf(Blocks.SMOKER)) && state.get(Properties.LIT);
    }
}
