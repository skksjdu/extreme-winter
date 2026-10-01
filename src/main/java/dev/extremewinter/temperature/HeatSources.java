package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.List;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.survival.HeatingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Indexed main heat sources, shared falloff/occlusion, bounded weak player heat scan once per second. */
public final class HeatSources {
    private final int radius;
    private final double maxStrength;
    private final double torchStrength;
    private final List<BlockPos> weakOffsets;
    public HeatSources(int radius) { this(radius, ExtremeWinter.CONFIG.maxHeatStrength, ExtremeWinter.CONFIG.torchHeatStrength); }
    public HeatSources(WinterConfig config) { this(config.heatSourceRadius, config.maxHeatStrength, config.torchHeatStrength); }
    private HeatSources(int radius, double maxStrength, double torchStrength) {
        this.radius = radius; this.maxStrength = maxStrength; this.torchStrength = torchStrength;
        var offsets = new ArrayList<BlockPos>();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
            if (x * x + y * y + z * z <= radius * radius) offsets.add(new BlockPos(x, y, z));
        }
        offsets.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        weakOffsets = List.copyOf(offsets);
    }
    public double strength(ServerPlayer player) {
        var world = player.level(); var origin = player.blockPosition();
        double total = 0;
        for (var pos : HeatSourceIndex.nearby(world, origin, Math.max(6, radius))) {
            total += contribution(world, player.position(), player.getEyePosition(), pos, CollisionContext.of(player));
            if (total >= maxStrength) return maxStrength;
        }
        for (var offset : weakOffsets) {
            var pos = origin.offset(offset);
            if (!loaded(world, pos)) continue;
            var state = world.getBlockState(pos);
            if (state.is(Blocks.LAVA) || HeatItems.isTorch(state)) {
                total += contribution(world, player.position(), player.getEyePosition(), pos, CollisionContext.of(player));
                if (total >= maxStrength) return maxStrength;
            }
        }
        return total;
    }
    /** Agricultural heat excludes torches/lava. The geometric contribution is the same as player heat. */
    public double strengthAt(ServerLevel world, BlockPos pos) { return HeatSourceIndex.sample(world, pos, this).strength(); }
    public boolean shelteredAt(ServerLevel world, BlockPos pos) { return HeatSourceIndex.sample(world, pos, this).sheltered(); }
    double uncachedStrengthAt(ServerLevel world, BlockPos origin) {
        if (!loaded(world, origin)) return 0;
        double total = 0; var target = Vec3.atCenterOf(origin);
        for (var pos : HeatSourceIndex.nearby(world, origin, Math.max(6, radius))) {
            total += contribution(world, target, target, pos, CollisionContext.empty());
            if (total >= maxStrength) return maxStrength;
        }
        return total;
    }
    private double contribution(ServerLevel world, Vec3 target, Vec3 sight, BlockPos source, CollisionContext context) {
        if (!loaded(world, source)) return 0;
        var state = world.getBlockState(source);
        if (!isHeatSource(state)) return 0;
        int range = state.is(HeatingContent.STOVE) ? 6 : radius;
        double distance = target.distanceTo(Vec3.atCenterOf(source));
        if (distance > range || !visible(world, sight, source, context)) return 0;
        double weight = state.is(HeatingContent.STOVE) ? 1.25 : HeatItems.isTorch(state) ? torchStrength : 1;
        return weight * (1 - .75 * distance / range);
    }
    public static boolean canCharge(ServerPlayer player) {
        for (var pos : HeatSourceIndex.nearby(player.level(), player.blockPosition(), 2)) {
            var state = player.level().getBlockState(pos);
            if (!(state.is(HeatingContent.STOVE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                    || !state.getValue(BlockStateProperties.LIT)) continue;
            if (player.position().add(0, .5, 0).distanceTo(Vec3.atCenterOf(pos)) <= 2
                    && visible(player.level(), player.getEyePosition(), pos, CollisionContext.of(player))) return true;
        }
        return false;
    }
    private static boolean loaded(ServerLevel world, BlockPos pos) { return world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null; }
    private static boolean visible(ServerLevel world, Vec3 from, BlockPos source, CollisionContext context) {
        var origin = BlockPos.containing(from);
        if (!world.hasChunksAt(Math.min(origin.getX(), source.getX()), Math.min(origin.getZ(), source.getZ()),
                Math.max(origin.getX(), source.getX()), Math.max(origin.getZ(), source.getZ()))) return false;
        var hit = world.clip(new ClipContext(from, Vec3.atCenterOf(source), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(source);
    }
    public static boolean isHeatSource(BlockState state) {
        return state.is(Blocks.LAVA) || HeatItems.isTorch(state)
                || (HeatSourceIndex.indexed(state) && state.getValue(BlockStateProperties.LIT));
    }
}
