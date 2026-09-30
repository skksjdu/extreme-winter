package dev.extremewinter;

import net.fabricmc.api.ModInitializer;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.config.ConfigFile;
import net.fabricmc.loader.api.FabricLoader;
import dev.extremewinter.temperature.TemperatureManager;
import dev.extremewinter.temperature.HeatWeathering;
import dev.extremewinter.temperature.HeatItems;
import dev.extremewinter.network.TemperatureSync;
import dev.extremewinter.environment.WinterClimate;
import dev.extremewinter.environment.WinterEnvironment;
import dev.extremewinter.environment.WinterBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.extremewinter.survival.StarterSupplies;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtremeWinter implements ModInitializer {
    public static final String ID = "extreme_winter";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final WinterConfig CONFIG = ConfigFile.load(
            FabricLoader.getInstance().getConfigDir().resolve("extreme-winter.json"));

    @Override
    public void onInitialize() {
        WinterBlocks.initialize();
        HeatItems.initialize();
        StarterSupplies.initialize();
        TemperatureData.initialize();
        TemperatureSync.register(CONFIG);
        WinterClimate.register(CONFIG);
        var environment = new WinterEnvironment(CONFIG);
        ServerLevelEvents.LOAD.register((server, world) -> environment.onLoad(world));
        ServerTickEvents.END_LEVEL_TICK.register(environment::tick);
        var weathering = new HeatWeathering(CONFIG);
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> weathering.onChunkLoad(world, chunk));
        ServerTickEvents.END_LEVEL_TICK.register(weathering::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> StarterSupplies.onJoin(handler.player));
        UseBlockCallback.EVENT.register(HeatItems::pickUpCampfire);
        ServerTickEvents.END_SERVER_TICK.register(HeatItems::tickInventories);
        ServerPlayerEvents.JOIN.register(TemperatureData::get);
        ServerTickEvents.END_SERVER_TICK.register(new TemperatureManager(CONFIG)::tick);
        LOGGER.info("Extreme Winter initialized for Minecraft 26.1");
    }
}
