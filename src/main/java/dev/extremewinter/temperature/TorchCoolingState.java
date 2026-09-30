package dev.extremewinter.temperature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/** Persist clocks by position, with a chunk index so ticks only visit nearby loaded torches. */
public final class TorchCoolingState extends PersistentState {
    private static final Codec<TorchCoolingState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(TorchCoolingState::new, state -> state.seconds);
    private static final PersistentStateType<TorchCoolingState> TYPE = new PersistentStateType<>(
            "extreme_winter_torch_cooling", () -> new TorchCoolingState(Map.of()), CODEC, null);
    private final Map<String, Integer> seconds;
    private final Map<Long, Set<String>> chunks = new HashMap<>();

    private TorchCoolingState(Map<String, Integer> saved) {
        seconds = new HashMap<>(saved);
        for (String key : saved.keySet()) index(key);
    }

    private void index(String key) {
        var pos = BlockPos.fromLong(Long.parseLong(key));
        chunks.computeIfAbsent(new ChunkPos(pos).toLong(), ignored -> new HashSet<>()).add(key);
    }

    public static TorchCoolingState get(ServerWorld world) { return world.getPersistentStateManager().getOrCreate(TYPE); }
    public int elapsed(BlockPos pos) { return seconds.getOrDefault(Long.toString(pos.asLong()), 0); }
    public boolean contains(BlockPos pos) { return seconds.containsKey(Long.toString(pos.asLong())); }
    public Set<String> inChunk(ChunkPos chunk) { return Set.copyOf(chunks.getOrDefault(chunk.toLong(), Set.of())); }

    public void track(BlockPos pos, int elapsed) {
        String key = Long.toString(pos.asLong());
        var previous = seconds.put(key, elapsed);
        if (previous == null) index(key);
        if (previous == null || previous != elapsed) markDirty();
    }

    public void remove(BlockPos pos) {
        String key = Long.toString(pos.asLong());
        if (seconds.remove(key) == null) return;
        long chunk = new ChunkPos(pos).toLong();
        var keys = chunks.get(chunk);
        keys.remove(key);
        if (keys.isEmpty()) chunks.remove(chunk);
        markDirty();
    }
}
