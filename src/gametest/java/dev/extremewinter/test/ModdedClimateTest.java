package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.SnowDriftBlock;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Optional production test: run with the user's Terralith/Tectonic dependencies. */
public final class ModdedClimateTest implements FabricClientGameTest {
    private static final BlockPos PROBE = new BlockPos(8, 80, 8);
    private static final BlockPos WATER = new BlockPos(5, 79, 10);
    private static final BlockPos COVERED = new BlockPos(12, 80, 10);

    @Override public void runTest(ClientGameTestContext context) {
        boolean enabled = Boolean.getBoolean("winter.test.moddedClimate");
        require(ExtremeWinter.CONFIG.coldModdedBiomes == enabled, "startup opt-in matches profile");
        int radius = ExtremeWinter.CONFIG.simulationRadiusChunks;
        int samples = ExtremeWinter.CONFIG.samplesPerPass;
        String weatherMode = ExtremeWinter.CONFIG.weatherMode;
        TestWorldSave save;
        try {
            ExtremeWinter.CONFIG.simulationRadiusChunks = 0;
            ExtremeWinter.CONFIG.samplesPerPass = 64;
            ExtremeWinter.CONFIG.weatherMode = "scheduled";
            try (var game = context.worldBuilder().setUseConsistentSettings(false)
                    .adjustSettings(creator -> creator.setSeed("20260930")).create()) {
                save = game.getWorldSave();
                game.getClientLevel().waitForChunksRender();
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    var registry = world.registryAccess().lookupOrThrow(Registries.BIOME);
                    var yellowstone = registry.getOrThrow(ResourceKey.create(Registries.BIOME,
                            Identifier.parse("terralith:yellowstone"))).value();
                    require(yellowstone.coldEnoughToSnow(PROBE, world.getSeaLevel()) == enabled,
                            "Yellowstone is warm by default and snowy only when opted in");
                    require(registry.getOrThrow(ResourceKey.create(Registries.BIOME,
                            Identifier.parse("minecraft:plains"))).value().getBaseTemperature() == -0.5f,
                            "vanilla Overworld climate remains cold");
                    require(registry.getOrThrow(ResourceKey.create(Registries.BIOME,
                            Identifier.parse("minecraft:nether_wastes"))).value().getBaseTemperature() == 2.0f,
                            "Nether climate stays unchanged");
                    require(registry.getOrThrow(ResourceKey.create(Registries.BIOME,
                            Identifier.parse("minecraft:the_end"))).value().getBaseTemperature() == 0.5f,
                            "End climate stays unchanged");
                    world.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
                    world.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
                    world.getChunk(0, 0);
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                        for (int y = 80; y <= world.getMaxY(); y++)
                            world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                        world.setBlock(new BlockPos(x, 79, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    }
                    for (int x = 11; x <= 13; x++) for (int z = 9; z <= 11; z++)
                        world.setBlock(new BlockPos(x, 84, z), Blocks.STONE.defaultBlockState(), 3);
                    world.setBlock(WATER, Blocks.WATER.defaultBlockState(), 3);
                    for(int x=3;x<=6;x++) for(int z=8;z<=11;z++)
                        world.setBlock(new BlockPos(x,79,z),Blocks.WATER.defaultBlockState(),3);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                            "fillbiome 0 60 0 15 127 15 terralith:yellowstone");
                    require(world.getBiome(PROBE).is(ResourceKey.create(Registries.BIOME,
                            Identifier.parse("terralith:yellowstone"))),
                            "fixture uses the user's exact warm Terralith biome");
                    ExposureTest.time(world, 6000);
                    ExposureTest.weather(world, 0, 12000, true, false);
                    dev.extremewinter.temperature.WinterWorldState.get(world).setElapsedTicks(90*1200);
                    var controller=new dev.extremewinter.environment.WinterWeatherController(ExtremeWinter.CONFIG);
                    controller.update(world);controller.applyVanilla(world);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    player.teleportTo(world, 8.5, 80, 2.5, Set.of(), 0, 12, true);
                    ExtremeWinter.LOGGER.info("TEST Terralith startup: optIn={}, temperature={}, precipitation={}",
                            enabled, yellowstone.getBaseTemperature(), yellowstone.getPrecipitationAt(PROBE, world.getSeaLevel()));
                });
                game.getClientLevel().waitForChunksRender();
                context.waitTicks(400);
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    int snow = snowLayers(world);
                    require(enabled ? snow > 0 : snow == 0, "real ticks accumulate snow only in cold climate: " + snow);
                    int sourceIce=0;
                    for(int x=3;x<=6;x++)for(int z=8;z<=11;z++)
                        if(world.getBlockState(new BlockPos(x,79,z)).is(Blocks.ICE))sourceIce++;
                    require(enabled ? sourceIce>0 : sourceIce==0,"exposed source water freezes only in cold climate: "+sourceIce);
                    require(world.getBlockState(COVERED).isAir(), "solid roof protects the floor from snow");
                    ExtremeWinter.LOGGER.info("TEST Terralith real ticks: optIn={}, snowLayers={}, water={}",
                            enabled, snow, world.getBlockState(WATER));
                    ExtremeWinter.LOGGER.info("TEST Terralith scheduled blizzard: sourceIce={}, extraSnowFactor={}, extraFreezeFactor={}",sourceIce,
                            dev.extremewinter.environment.WinterWeatherController.snowFactor(world,ExtremeWinter.CONFIG),
                            dev.extremewinter.environment.WinterWeatherController.freezingFactor(world,ExtremeWinter.CONFIG));
                });
                context.runOnClient(client->{
                    dev.extremewinter.client.hud.TemperatureHud.updateWinter(new dev.extremewinter.network.WinterStatusPayload(1,3,0,180,90*1200));
                    int particles=dev.extremewinter.client.SnowstormEffects.emit(client.level,PROBE,net.minecraft.util.RandomSource.create(42));
                    require(enabled?particles>0:particles==0,"extra storm snowflakes follow actual cold biome opt-in");
                });
                context.runOnClient(client -> require(client.level.getBiome(PROBE).value()
                        .getPrecipitationAt(PROBE, client.level.getSeaLevel())
                        == (enabled ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN),
                        "client registry renders snow instead of rain"));
                context.runOnClient(client -> {
                    try {
                        Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                        Object iris = api.getMethod("getInstance").invoke(null);
                        require((boolean) api.getMethod("isShaderPackInUse").invoke(iris), "Eclipse shader is actually active");
                        String pack = (String) Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName").invoke(null);
                        require(pack.equals("Eclipse-Shader-Unstable.zip"), "the active shader is the supplied Eclipse pack: " + pack);
                        ExtremeWinter.LOGGER.info("TEST Terralith Iris shader active=true pack={} optIn={}", pack, enabled);
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Could not verify actual Eclipse shader activation", exception);
                    }
                });
                context.takeScreenshot("terralith-climate-" + (enabled ? "snow" : "rain"));
            }
            try (var game = save.open()) {
                game.getClientLevel().waitForChunksRender();
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    require(world.getBiome(PROBE).value().coldEnoughToSnow(PROBE, world.getSeaLevel()) == enabled,
                            "existing saved biome receives climate again on reload");
                    require(enabled ? snowLayers(world) > 0 : snowLayers(world) == 0,
                            "snow is preserved through world reload");
                });
            }
        } finally {
            ExtremeWinter.CONFIG.simulationRadiusChunks = radius;
            ExtremeWinter.CONFIG.samplesPerPass = samples;
            ExtremeWinter.CONFIG.weatherMode = weatherMode;
        }
        ExtremeWinter.LOGGER.info("TEST Terralith climate optIn={}: client precipitation, real snow/ice ticks, roof, dimensions and reload PASSED", enabled);
    }

    private static int snowLayers(ServerLevel world) {
        int result = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 80; y <= 83; y++)
            result += SnowDriftBlock.layers(world.getBlockState(new BlockPos(x, y, z)));
        return result;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
