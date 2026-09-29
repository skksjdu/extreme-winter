package dev.extremewinter.temperature;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Immutable, saved in the vanilla player data. No copyOnDeath: respawn is warm. */
public final class TemperatureData {
    public static final AttachmentType<Double> TEMPERATURE = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "temperature"), builder -> builder
                    .initializer(() -> ExtremeWinter.CONFIG.maxTemperature)
                    .persistent(Codec.DOUBLE));

    private TemperatureData() { }

    public static double get(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(TEMPERATURE);
    }

    public static void set(ServerPlayerEntity player, double temperature) {
        player.setAttached(TEMPERATURE, temperature);
    }

    public static void initialize() { }
}
