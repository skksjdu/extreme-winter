package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;

/** Bounded scan once per player per second, nearest first, never loads chunks. */
public final class HeatSources {
    private final int radius;
    private final double maxStrength;
    private final double torchStrength;
    private final List<BlockPos> offsets;

    public HeatSources(int radius) {
        this(radius, ExtremeWinter.CONFIG.maxHeatStrength, ExtremeWinter.CONFIG.torchHeatStrength);
    }

    public HeatSources(WinterConfig config) {
        this(config.heatSourceRadius, config.maxHeatStrength, config.torchHeatStrength);
    }

    private HeatSources(int radius, double maxStrength, double torchStrength) {
        this.radius = radius;
        this.maxStrength = maxStrength;
        this.torchStrength = torchStrength;
        var positions = new ArrayList<BlockPos>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) positions.add(new BlockPos(x, y, z));
                }
            }
        }
        positions.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        offsets = List.copyOf(positions);
    }

    public double strength(ServerPlayer player) {
        ServerLevel world = player.level();
        BlockPos origin = player.blockPosition();
        double total = 0;
        for (BlockPos offset : offsets) {
            BlockPos pos = origin.offset(offset);
            if (world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            var state = world.getBlockState(pos);
            if (!isHeatSource(state)) continue;
            double distance = player.position().distanceTo(Vec3.atCenterOf(pos));
            if (distance > radius) continue;
            // Check the small ray corridor before raycasting, so a chunk border cannot cause a load.
            if (!world.hasChunksAt(Math.min(pos.getX(), origin.getX()), Math.min(pos.getZ(), origin.getZ()),
                    Math.max(pos.getX(), origin.getX()), Math.max(pos.getZ(), origin.getZ()))) continue;
            var hit = world.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                double weight = HeatItems.isTorch(state) ? torchStrength : 1;
                total += weight * (1 - 0.75 * distance / radius);
                if (total >= maxStrength) return maxStrength;
            }
        }
        return total;
    }

    public static boolean isHeatSource(BlockState state) {
        if (state.is(Blocks.LAVA) || HeatItems.isTorch(state)) return true;
        return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)) && state.getValue(BlockStateProperties.LIT);
    }
}
