package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.network.TemperatureSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public final class TemperatureManager {
    private final WinterConfig config;
    private final HeatSources heatSources;

    public TemperatureManager(WinterConfig config) {
        this.config = config;
        this.heatSources = new HeatSources(config);
    }

    public void tick(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            // Spread player work across 20 ticks. No heat or block scans every tick.
            if (Math.floorMod(server.getTickCount() + player.getId(), 20) != 0 || !player.isAlive()) continue;
            double previous = TemperatureData.get(player);
            double value;
            var world = player.level();
            if (player.isCreative() || player.isSpectator()) {
                value = config.maxTemperature;
            } else if (!world.dimension().equals(Level.OVERWORLD)) {
                value = TemperatureModel.clamp(previous + config.recoveryRate, config);
            } else {
                value = TemperatureModel.step(previous, Exposure.outdoors(world, player.blockPosition().above()),
                        world.isRaining(), world.isDarkOutside(), player.isInWater(), heatSources.strength(player),
                        WinterProgression.lossMultiplier(world.getOverworldClockTime(), config), config);
            }
            TemperatureData.set(player, value);
            Hypothermia.apply(player, value, server.getTickCount(), config);
            TemperatureSync.send(player, config);
        }
    }
}
