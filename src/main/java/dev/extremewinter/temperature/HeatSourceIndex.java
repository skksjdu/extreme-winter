package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.survival.HeatingContent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Only loaded chunks participate; no saved or background simulation of unloaded sources. */
public final class HeatSourceIndex {
    private static final Map<ServerLevel, Data> WORLDS = new WeakHashMap<>();
    private static final int CACHE_LIMIT = 4096;
    private static final class Data {
        final Map<Long, Set<BlockPos>> chunks = new HashMap<>();
        final LinkedHashMap<Long, ThermalSample> cache = new LinkedHashMap<>(64, .75f, true);
        long generation;
    }
    public record ThermalSample(double strength, boolean sheltered, long expires, long generation) { }
    private HeatSourceIndex() { }
    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> onLoad(world, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register(HeatSourceIndex::onUnload);
        ServerLevelEvents.UNLOAD.register((server, world) -> WORLDS.remove(world));
    }
    public static boolean indexed(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER) || state.is(HeatingContent.STOVE);
    }
    public static void onLoad(ServerLevel world, LevelChunk chunk) {
        var data = WORLDS.computeIfAbsent(world, ignored -> new Data());
        var sources = new HashSet<BlockPos>();
        for (var pos : chunk.getBlockEntitiesPos()) {
            if (indexed(chunk.getBlockState(pos))) sources.add(pos.immutable());
        }
        if (sources.isEmpty()) data.chunks.remove(chunk.getPos().pack());
        else data.chunks.put(chunk.getPos().pack(), sources);
        data.generation++;
        data.cache.clear();
    }
    public static void onUnload(ServerLevel world, LevelChunk chunk) {
        var data = WORLDS.get(world);
        if (data != null) { data.chunks.remove(chunk.getPos().pack()); data.generation++; data.cache.clear(); }
    }
    public static void changed(ServerLevel world, BlockPos pos, BlockState state) {
        var data = WORLDS.computeIfAbsent(world, ignored -> new Data());
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        if (indexed(state)) data.chunks.computeIfAbsent(key, ignored -> new HashSet<>()).add(pos.immutable());
        else {
            var sources = data.chunks.get(key);
            if (sources != null) { sources.remove(pos); if (sources.isEmpty()) data.chunks.remove(key); }
        }
        // Lighting/extinguishing also changes contributions even though membership stays the same.
        data.generation++;
    }
    public static List<BlockPos> nearby(ServerLevel world, BlockPos origin, int radius) {
        var data = WORLDS.get(world);
        if (data == null) return List.of();
        var found = new ArrayList<BlockPos>();
        for (int x = (origin.getX() - radius) >> 4; x <= (origin.getX() + radius) >> 4; x++) {
            for (int z = (origin.getZ() - radius) >> 4; z <= (origin.getZ() + radius) >> 4; z++) {
                if (world.getChunkSource().getChunkNow(x, z) == null) continue;
                var sources = data.chunks.get(ChunkPos.pack(x, z));
                if (sources != null) found.addAll(sources);
            }
        }
        found.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        return found;
    }
    public static ThermalSample sample(ServerLevel world, BlockPos pos, HeatSources heat) {
        var data = WORLDS.computeIfAbsent(world, ignored -> new Data());
        var cached = data.cache.get(pos.asLong());
        long now = world.getGameTime();
        if (cached != null && cached.expires > now && cached.generation == data.generation) return cached;
        var result = new ThermalSample(heat.uncachedStrengthAt(world, pos), !Exposure.outdoors(world, pos.above()), now + 40, data.generation);
        data.cache.put(pos.asLong(), result);
        if (data.cache.size() > CACHE_LIMIT) data.cache.remove(data.cache.keySet().iterator().next());
        return result;
    }
}
