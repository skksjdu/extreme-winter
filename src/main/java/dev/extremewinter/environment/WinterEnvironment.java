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
            if (SnowDriftBlock.layers(world.getBlockState(surface.down())) > 0) surface = surface.down();
            if (freeze) tryFreeze(world, surface.down());
            if (snow) trySnow(world, surface);
        }
    }

    public boolean trySnow(ServerWorld world, BlockPos pos) {
        if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null
                || !world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)) return false;
        // Accept a sampled snow position or the air immediately above an existing drift.
        BlockPos top = pos;
        if (world.getBlockState(top).isAir() && SnowDriftBlock.layers(world.getBlockState(top.down())) > 0) top = top.down();
        while (SnowDriftBlock.layers(world.getBlockState(top.up())) > 0 && world.isInBuildLimit(top.up())) top = top.up();
        if (!isExposedSurface(world, top) || world.getLightLevel(LightType.BLOCK, top) >= 10
                || !world.getBiome(top).value().isCold(top, world.getSeaLevel())) return false;
        var state = world.getBlockState(top);
        int layers = SnowDriftBlock.layers(state);
        if (layers > 0) {
            int total = layers;
            BlockPos lower = top.down();
            // A bounded column walk, never a chunk-wide scan.
            while (total < config.maxSnowLayers && SnowDriftBlock.layers(world.getBlockState(lower)) > 0) {
                total += SnowDriftBlock.layers(world.getBlockState(lower));
                lower = lower.down();
            }
            if (total >= config.maxSnowLayers) return false;
            if (layers < 8) return world.setBlockState(top,
                    WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, layers + 1), Block.NOTIFY_ALL);
            BlockPos above = top.up();
            if (!isExposedSurface(world, above) || !world.getBlockState(above).isAir()) return false;
            return world.setBlockState(above, WinterBlocks.SNOW_DRIFT.getDefaultState(), Block.NOTIFY_ALL);
        }
        // Never replace crops, grass, machines, waterlogged blocks, or any non-air block.
        if (!state.isAir()) return false;
        var below = world.getBlockState(pos.down());
        if (below.hasBlockEntity() || !below.isIn(SNOW_SURFACES)) return false;
        var snow = WinterBlocks.SNOW_DRIFT.getDefaultState();
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
