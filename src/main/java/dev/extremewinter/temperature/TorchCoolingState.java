package dev.extremewinter.temperature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;

/** Persist clocks by position, with a chunk index so ticks only visit nearby loaded torches. */
public final class TorchCoolingState extends SavedData {
    private static final Codec<TorchCoolingState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(TorchCoolingState::new, state -> state.seconds);
    private static final SavedDataType<TorchCoolingState> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("extreme_winter_torch_cooling"), () -> new TorchCoolingState(Map.of()), CODEC, null);
    private final Map<String, Integer> seconds;
    private final Map<Long, Set<String>> chunks = new HashMap<>();

    private TorchCoolingState(Map<String, Integer> saved) {
        seconds = new HashMap<>(saved);
        for (String key : saved.keySet()) index(key);
    }

    private void index(String key) {
        var pos = BlockPos.of(Long.parseLong(key));
        chunks.computeIfAbsent(ChunkPos.containing(pos).pack(), ignored -> new HashSet<>()).add(key);
    }

    public static TorchCoolingState get(ServerLevel world) { return world.getDataStorage().computeIfAbsent(TYPE); }
    public int elapsed(BlockPos pos) { return seconds.getOrDefault(Long.toString(pos.asLong()), 0); }
    public boolean contains(BlockPos pos) { return seconds.containsKey(Long.toString(pos.asLong())); }
    public Set<String> inChunk(ChunkPos chunk) { return Set.copyOf(chunks.getOrDefault(chunk.pack(), Set.of())); }

    public void track(BlockPos pos, int elapsed) {
        String key = Long.toString(pos.asLong());
        var previous = seconds.put(key, elapsed);
        if (previous == null) index(key);
        if (previous == null || previous != elapsed) setDirty();
    }

    public void remove(BlockPos pos) {
        String key = Long.toString(pos.asLong());
        if (seconds.remove(key) == null) return;
        long chunk = ChunkPos.containing(pos).pack();
        var keys = chunks.get(chunk);
        keys.remove(key);
        if (keys.isEmpty()) chunks.remove(chunk);
        setDirty();
    }
}
