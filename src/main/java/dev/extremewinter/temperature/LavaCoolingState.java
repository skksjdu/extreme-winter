package dev.extremewinter.temperature;

import java.util.HashMap;
import java.util.Map;
import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/** Only discovered source positions, never a world-wide block scan or an offline timer. */
public final class LavaCoolingState extends PersistentState {
    private static final Codec<LavaCoolingState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(LavaCoolingState::new, state -> state.seconds);
    private static final PersistentStateType<LavaCoolingState> TYPE = new PersistentStateType<>(
            "extreme_winter_lava_cooling", () -> new LavaCoolingState(Map.of()), CODEC, null);
    final Map<String, Integer> seconds;

    private LavaCoolingState(Map<String, Integer> seconds) { this.seconds = new HashMap<>(seconds); }
    public static LavaCoolingState get(ServerWorld world) { return world.getPersistentStateManager().getOrCreate(TYPE); }
}
