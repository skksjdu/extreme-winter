package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.network.TemperatureSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public final class TemperatureManager {
    private final WinterConfig config;
    private final HeatSources heatSources;

    public TemperatureManager(WinterConfig config) {
        this.config = config;
        this.heatSources = new HeatSources(config.heatSourceRadius);
    }

    public void tick(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            // Spread player work across 20 ticks. No heat or block scans every tick.
            if (Math.floorMod(server.getTicks() + player.getId(), 20) != 0 || !player.isAlive()) continue;
            double previous = TemperatureData.get(player);
            double value;
            var world = player.getWorld();
            if (player.isCreative() || player.isSpectator()) {
                value = config.maxTemperature;
            } else if (!world.getRegistryKey().equals(World.OVERWORLD)) {
                value = TemperatureModel.clamp(previous + config.recoveryRate, config);
            } else {
                value = TemperatureModel.step(previous, Exposure.outdoors(world, player.getBlockPos()),
                        world.isRaining(), world.isNight(), player.isTouchingWater(), heatSources.strength(player), config);
            }
            TemperatureData.set(player, value);
            Hypothermia.apply(player, value, server.getTicks(), config);
            TemperatureSync.send(player, config);
        }
    }
}
