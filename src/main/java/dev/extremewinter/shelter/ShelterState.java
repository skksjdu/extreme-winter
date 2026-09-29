package dev.extremewinter.shelter;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/** Saved in the Overworld data directory, not a process-global generated flag. */
public final class ShelterState extends PersistentState {
    private static final Codec<ShelterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("outcome", "pending").forGetter(state -> state.outcome),
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(state -> Optional.ofNullable(state.origin))
    ).apply(instance, ShelterState::new));
    private static final PersistentStateType<ShelterState> TYPE = new PersistentStateType<>(
            "extreme_winter_shelter", () -> new ShelterState("pending", Optional.empty()), CODEC, null);

    private String outcome;
    private BlockPos origin;

    private ShelterState(String outcome, Optional<BlockPos> origin) {
        this.outcome = outcome;
        this.origin = origin.orElse(null);
    }

    public static ShelterState get(ServerWorld world) { return world.getPersistentStateManager().getOrCreate(TYPE); }
    public String outcome() { return outcome; }
    public BlockPos origin() { return origin; }
    public boolean generated() { return outcome.equals("generated") && origin != null; }

    public void finish(String result, BlockPos pos) {
        outcome = result;
        origin = pos;
        markDirty();
    }
}
