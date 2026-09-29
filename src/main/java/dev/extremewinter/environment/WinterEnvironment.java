package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public final class WinterEnvironment {
    public static final TagKey<Block> SNOW_SURFACES = TagKey.of(RegistryKeys.BLOCK,
            Identifier.of(ExtremeWinter.ID, "snow_surfaces"));
    private final WinterConfig config;
    private int playerCursor;

    public WinterEnvironment(WinterConfig config) { this.config = config; }

    public void onLoad(ServerWorld world) {
        if (world.getRegistryKey().equals(World.OVERWORLD) && config.persistentWeather) {
            world.setWeather(0, 12000, true, false);
        }
    }

    public void tick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (config.persistentWeather && world.getTime() % 1200 == 0) onLoad(world);
        boolean snow = config.snowAccumulation && world.getTime() % config.snowIntervalTicks == 0;
        boolean freeze = config.waterFreezing && world.getTime() % config.freezeIntervalTicks == 0;
        if (snow || freeze) sample(world, snow, freeze);
    }

    private void sample(ServerWorld world, boolean snow, boolean freeze) {
        var players = world.getPlayers();
        if (players.isEmpty()) return;
        int radius = config.simulationRadiusChunks;
        for (int i = 0; i < config.samplesPerPass; i++) {
            var player = players.get(Math.floorMod(playerCursor++, players.size()));
            if (player.isSpectator()) continue;
            int cx = (player.getBlockX() >> 4) + world.random.nextInt(2 * radius + 1) - radius;
            int cz = (player.getBlockZ() >> 4) + world.random.nextInt(2 * radius + 1) - radius;
            var chunk = world.getChunkManager().getWorldChunk(cx, cz);
            if (chunk == null) continue;
            int x = world.random.nextInt(16);
            int z = world.random.nextInt(16);
            int y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x, z) + 1;
            BlockPos surface = new BlockPos(cx * 16 + x, y, cz * 16 + z);
            if (world.getBlockState(surface.down()).isOf(Blocks.SNOW)) surface = surface.down();
            if (freeze) tryFreeze(world, surface.down());
            if (snow) trySnow(world, surface);
        }
    }

    public boolean trySnow(ServerWorld world, BlockPos pos) {
        if (!isExposedSurface(world, pos) || world.getLightLevel(LightType.BLOCK, pos) >= 10
                || !world.getBiome(pos).value().isCold(pos, world.getSeaLevel())) return false;
        var state = world.getBlockState(pos);
        if (state.isOf(Blocks.SNOW)) {
            int layers = state.get(SnowBlock.LAYERS);
            if (layers >= config.maxSnowLayers) return false;
            return world.setBlockState(pos, state.with(SnowBlock.LAYERS, layers + 1), Block.NOTIFY_ALL);
        }
        // Never replace crops, grass, machines, waterlogged blocks, or any non-air block.
        if (!state.isAir()) return false;
        var below = world.getBlockState(pos.down());
        if (below.hasBlockEntity() || !below.isIn(SNOW_SURFACES)) return false;
        var snow = Blocks.SNOW.getDefaultState();
        if (!snow.canPlaceAt(world, pos)) return false;
        return world.setBlockState(pos, snow, Block.NOTIFY_ALL);
    }

    public static boolean isExposedSurface(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) != null
                && world.isInBuildLimit(pos) && world.getWorldBorder().contains(pos)
                && world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1
                && world.isSkyVisible(pos.up());
    }

    public boolean tryFreeze(ServerWorld world, BlockPos pos) {
        if (!isExposedSurface(world, pos) || world.getLightLevel(LightType.BLOCK, pos) >= 10
                || !world.getBiome(pos).value().isCold(pos, world.getSeaLevel())) return false;
        var state = world.getBlockState(pos);
        // Source water only. Never replace flowing water or a waterlogged crop/machine/block.
        if (!state.isOf(Blocks.WATER) || !state.getFluidState().isStill()) return false;
        return world.setBlockState(pos, Blocks.ICE.getDefaultState(), Block.NOTIFY_ALL);
    }
}
