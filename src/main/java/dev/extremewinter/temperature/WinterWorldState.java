package dev.extremewinter.temperature;
import java.util.HashMap;
import java.util.Map;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One clock in Overworld storage, shared by all dimensions. No offline catch-up. */
public final class WinterWorldState extends SavedData {
    private static final Codec<WinterWorldState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(WinterWorldState::new, state -> state.values);
    private static final SavedDataType<WinterWorldState> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("extreme_winter_world"), () -> new WinterWorldState(Map.of()), CODEC, null);
    private final Map<String, Long> values;
    private WinterWorldState(Map<String, Long> values) {
        this.values = new HashMap<>(values);
        this.values.putIfAbsent("schemaVersion", 1L);
        this.values.putIfAbsent("elapsedTicks", 0L);
    }
    public static WinterWorldState get(ServerLevel world) { return world.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
    public long elapsedTicks() { return Math.max(0, value("elapsedTicks", 0)); }
    public long value(String key, long fallback) { return values.getOrDefault(key, fallback); }
    public void put(String key, long value) { if (!Long.valueOf(value).equals(values.put(key, value))) setDirty(); }
    public void setElapsedTicks(long ticks) { put("elapsedTicks", Math.max(0, ticks)); }
    public static void tick(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().stream().noneMatch(p -> p.isAlive() && !p.isSpectator())) return;
        var state = get(server.overworld());
        if (state.elapsedTicks() < Long.MAX_VALUE) state.setElapsedTicks(state.elapsedTicks() + 1);
        state.put("winterStage", WinterProgression.stage(state.elapsedTicks(), dev.extremewinter.ExtremeWinter.CONFIG));
    }
}
