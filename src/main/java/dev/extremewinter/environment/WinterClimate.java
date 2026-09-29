package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.util.Identifier;

public final class WinterClimate {
    private WinterClimate() { }

    public static void register(WinterConfig config) {
        if (!config.coldVanillaBiomes) return;
        // Public registry modification, not a biome replacement or renderer hook.
        // Modded biomes retain their authors' climate. Can be disabled in the config.
        BiomeModifications.create(Identifier.of(ExtremeWinter.ID, "winter_climate")).add(
                ModificationPhase.POST_PROCESSING,
                BiomeSelectors.foundInOverworld().and(context ->
                        context.getBiomeKey().getValue().getNamespace().equals("minecraft")),
                context -> {
                    context.getWeather().setTemperature(-0.5f);
                    context.getWeather().setPrecipitation(true);
                });
    }
}
