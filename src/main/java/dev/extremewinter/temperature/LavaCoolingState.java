package dev.extremewinter.temperature;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;

/** Only discovered source positions, never a world-wide block scan or an offline timer. */
public final class LavaCoolingState extends SavedData {
    private static final Codec<LavaCoolingState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(LavaCoolingState::new, state -> state.seconds);
    private static final SavedDataType<LavaCoolingState> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace("extreme_winter_lava_cooling"), () -> new LavaCoolingState(Map.of()), CODEC, null);
    final Map<String, Integer> seconds;

    private LavaCoolingState(Map<String, Integer> seconds) { this.seconds = new HashMap<>(seconds); }
    public static LavaCoolingState get(ServerLevel world) { return world.getDataStorage().computeIfAbsent(TYPE); }
}
