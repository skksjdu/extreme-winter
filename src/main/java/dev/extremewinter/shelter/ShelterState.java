package dev.extremewinter.shelter;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.util.BlockRotation;

/** Saved in the Overworld data directory, not a process-global generated flag. */
public final class ShelterState extends PersistentState {
    private static final Codec<ShelterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("outcome", "pending").forGetter(state -> state.outcome),
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(state -> Optional.ofNullable(state.origin)),
            BlockRotation.CODEC.optionalFieldOf("rotation", BlockRotation.NONE).forGetter(state -> state.rotation)
    ).apply(instance, ShelterState::new));
    private static final PersistentStateType<ShelterState> TYPE = new PersistentStateType<>(
            "extreme_winter_shelter", () -> new ShelterState("pending", Optional.empty(), BlockRotation.NONE), CODEC, null);

    private String outcome;
    private BlockPos origin;
    private BlockRotation rotation;

    private ShelterState(String outcome, Optional<BlockPos> origin, BlockRotation rotation) {
        this.outcome = outcome;
        this.origin = origin.orElse(null);
        this.rotation = rotation;
    }

    public static ShelterState get(ServerWorld world) { return world.getPersistentStateManager().getOrCreate(TYPE); }
    public String outcome() { return outcome; }
    public BlockPos origin() { return origin; }
    public boolean generated() { return outcome.equals("generated") && origin != null; }
    public BlockPos at(int x, int y, int z) { return origin.add(new BlockPos(x, y, z).rotate(rotation)); }

    public void finish(String result, BlockPos pos) {
        outcome = result;
        origin = pos;
        markDirty();
    }
    public void setRotation(BlockRotation value) { rotation = value; markDirty(); }
    public float arrivalYaw() { return switch (rotation) {
        case NONE -> 180;
        case CLOCKWISE_90 -> 270;
        case CLOCKWISE_180 -> 0;
        case COUNTERCLOCKWISE_90 -> 90;
    }; }
}
