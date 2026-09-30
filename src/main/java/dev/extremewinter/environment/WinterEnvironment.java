package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.Heightmap;

public final class WinterEnvironment {
    public static final TagKey<Block> SNOW_SURFACES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "snow_surfaces"));
    private final WinterConfig config;
    private int playerCursor;

    public WinterEnvironment(WinterConfig config) { this.config = config; }

    public void onLoad(ServerLevel world) {
        if (world.dimension().equals(Level.OVERWORLD) && config.persistentWeather) {
            var weather = world.getWeatherData();
            weather.setClearWeatherTime(0);
            weather.setRainTime(12000);
            weather.setThunderTime(12000);
            weather.setRaining(true);
            weather.setThundering(false);
        }
    }

    public void tick(ServerLevel world) {
        if (!world.dimension().equals(Level.OVERWORLD)) return;
        if (config.persistentWeather && world.getGameTime() % 1200 == 0) onLoad(world);
        boolean snow = config.snowAccumulation && world.getGameTime() % config.snowIntervalTicks == 0;
        boolean freeze = config.waterFreezing && world.getGameTime() % config.freezeIntervalTicks == 0;
        if (snow || freeze) sample(world, snow, freeze);
    }

    private void sample(ServerLevel world, boolean snow, boolean freeze) {
        var players = world.players();
        if (players.isEmpty()) return;
        int radius = config.simulationRadiusChunks;
        for (int i = 0; i < config.samplesPerPass; i++) {
            var player = players.get(Math.floorMod(playerCursor++, players.size()));
            if (player.isSpectator()) continue;
            int cx = (player.getBlockX() >> 4) + world.getRandom().nextInt(2 * radius + 1) - radius;
            int cz = (player.getBlockZ() >> 4) + world.getRandom().nextInt(2 * radius + 1) - radius;
            var chunk = world.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;
            int x = world.getRandom().nextInt(16);
            int z = world.getRandom().nextInt(16);
            int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
            BlockPos surface = new BlockPos(cx * 16 + x, y, cz * 16 + z);
            if (SnowDriftBlock.layers(world.getBlockState(surface.below())) > 0) surface = surface.below();
            if (freeze) tryFreeze(world, surface.below());
            if (snow) trySnow(world, surface);
        }
    }

    public boolean trySnow(ServerLevel world, BlockPos pos) {
        if (world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                || !world.isInWorldBounds(pos) || !world.getWorldBorder().isWithinBounds(pos)) return false;
        // Accept a sampled snow position or the air immediately above an existing drift.
        BlockPos top = pos;
        if (world.getBlockState(top).isAir() && SnowDriftBlock.layers(world.getBlockState(top.below())) > 0) top = top.below();
        while (SnowDriftBlock.layers(world.getBlockState(top.above())) > 0 && world.isInWorldBounds(top.above())) top = top.above();
        if (!isExposedSurface(world, top) || world.getBrightness(LightLayer.BLOCK, top) >= 10
                || !world.getBiome(top).value().coldEnoughToSnow(top, world.getSeaLevel())) return false;
        var state = world.getBlockState(top);
        int layers = SnowDriftBlock.layers(state);
        if (layers > 0) {
            int total = layers;
            BlockPos lower = top.below();
            // A bounded column walk, never a chunk-wide scan.
            while (total < config.maxSnowLayers && SnowDriftBlock.layers(world.getBlockState(lower)) > 0) {
                total += SnowDriftBlock.layers(world.getBlockState(lower));
                lower = lower.below();
            }
            if (total >= config.maxSnowLayers) return false;
            if (layers < 8) return world.setBlock(top,
                    WinterBlocks.SNOW_DRIFT.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers + 1), Block.UPDATE_ALL);
            BlockPos above = top.above();
            if (!isExposedSurface(world, above) || !world.getBlockState(above).isAir()) return false;
            return world.setBlock(above, WinterBlocks.SNOW_DRIFT.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Never replace crops, grass, machines, waterlogged blocks, or any non-air block.
        if (!state.isAir()) return false;
        var below = world.getBlockState(pos.below());
        if (below.hasBlockEntity() || !below.is(SNOW_SURFACES)) return false;
        var snow = WinterBlocks.SNOW_DRIFT.defaultBlockState();
        if (!snow.canSurvive(world, pos)) return false;
        return world.setBlock(pos, snow, Block.UPDATE_ALL);
    }

    public static boolean isExposedSurface(ServerLevel world, BlockPos pos) {
        return world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && world.isInWorldBounds(pos) && world.getWorldBorder().isWithinBounds(pos)
                && world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1
                && world.canSeeSky(pos.above());
    }

    public boolean tryFreeze(ServerLevel world, BlockPos pos) {
        if (!isExposedSurface(world, pos) || world.getBrightness(LightLayer.BLOCK, pos) >= 10
                || !world.getBiome(pos).value().coldEnoughToSnow(pos, world.getSeaLevel())) return false;
        var state = world.getBlockState(pos);
        // Source water only. Never replace flowing water or a waterlogged crop/machine/block.
        if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) return false;
        return world.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
    }
}
