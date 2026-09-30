package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.Identifier;

public final class WinterClimate {
    private WinterClimate() { }

    public static void register(WinterConfig config) {
        if (!config.coldVanillaBiomes && !config.coldModdedBiomes) return;
        // Public registry modification, not a biome replacement or renderer hook.
        // Modded Overworld climates are an explicit opt-in; other dimensions stay intact.
        BiomeModifications.create(Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "winter_climate")).add(
                ModificationPhase.POST_PROCESSING,
                BiomeSelectors.foundInOverworld().and(context ->
                        context.getBiomeKey().identifier().getNamespace().equals("minecraft")
                                ? config.coldVanillaBiomes : config.coldModdedBiomes),
                context -> {
                    context.getWeather().setTemperature(-0.5f);
                    context.getWeather().setPrecipitation(true);
                });
    }
}
