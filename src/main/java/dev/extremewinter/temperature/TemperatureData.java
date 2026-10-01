package dev.extremewinter.temperature;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Immutable, saved in the vanilla player data. No copyOnDeath: respawn is warm. */
public final class TemperatureData {
    public static final AttachmentType<Double> TEMPERATURE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "temperature"), builder -> builder
                    .initializer(() -> ExtremeWinter.CONFIG.maxTemperature)
                    .persistent(Codec.DOUBLE));

    private TemperatureData() { }
    public static final AttachmentType<Integer> LOW_TICKS = timer("low_temperature_ticks", 0);
    public static final AttachmentType<Integer> PROTECTION = timer("cold_protection_ticks", ExtremeWinter.CONFIG.respawnProtectionSeconds * 20);
    public static final AttachmentType<Integer> FREEZE_COOLDOWN = timer("vanilla_freeze_cooldown", 0);
    public static final AttachmentType<Integer> DAMAGE_TICKS = timer("cold_damage_ticks", 0);
    public static final AttachmentType<Boolean> WARNED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "cold_warning_shown"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    private static AttachmentType<Integer> timer(String name, int initial) {
        return AttachmentRegistry.create(Identifier.fromNamespaceAndPath(ExtremeWinter.ID, name),
                b -> b.initializer(() -> initial).persistent(Codec.intRange(0, 72000)));
    }
    public static void tickTimers(ServerPlayer player) {
        for (var type : java.util.List.of(PROTECTION, FREEZE_COOLDOWN)) {
            int value = player.getAttachedOrCreate(type);
            if (value > 0) player.setAttached(type, value - 1);
        }
    }

    public static double get(ServerPlayer player) {
        return player.getAttachedOrCreate(TEMPERATURE);
    }

    public static void set(ServerPlayer player, double temperature) {
        player.setAttached(TEMPERATURE, temperature);
    }

    public static void initialize() { }
}
