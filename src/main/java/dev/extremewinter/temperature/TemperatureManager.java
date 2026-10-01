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
            if (player.isAlive() && !player.isSpectator()) TemperatureData.tickTimers(player);
            // Spread player work across 20 ticks. No heat or block scans every tick.
            if (Math.floorMod(server.getTickCount() + player.getId(), 20) != 0 || !player.isAlive()) continue;
            double previous = TemperatureData.get(player);
            double value;
            boolean outdoors = false;
            double heat = 0;
            var world = player.level();
            if (player.isCreative() || player.isSpectator()) {
                value = config.maxTemperature;
            } else if (!world.dimension().equals(Level.OVERWORLD)) {
                value = TemperatureModel.clamp(previous + config.recoveryRate, config);
            } else {
                outdoors = Exposure.outdoors(world, player.blockPosition().above());
                heat = heatSources.strength(player);
                long elapsed = WinterWorldState.get(world).elapsedTicks();
                value = TemperatureModel.step(previous, outdoors,
                        world.isRaining(), world.isDarkOutside(), player.isInWater(), heat,
                        WinterProgression.lossMultiplier(elapsed, config),
                        dev.extremewinter.environment.WinterWeatherController.airMultiplier(world, outdoors, config)
                                * dev.extremewinter.survival.WinterGear.airMultiplier(player), config);
                value = TemperatureModel.clamp(value + dev.extremewinter.survival.WinterGear.portableGain(player), config);
                if (WinterProgression.stage(elapsed, config) == 0) value = Math.max(config.coldThreshold, value);
            }
            TemperatureData.set(player, value);
            dev.extremewinter.survival.WinterTasks.warmed(player, previous, value, heat);
            Hypothermia.apply(player, value, server.getTickCount(), config);
            TemperatureSync.send(player, config, Double.compare(value, previous), (outdoors ? 0 : 1) | (heat > 0 ? 2 : 0) | (player.isInWater() ? 4 : 0));
        }
    }
}
