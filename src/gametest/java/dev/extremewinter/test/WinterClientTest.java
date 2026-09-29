package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.environment.WinterEnvironment;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.shelter.ShelterState;
import dev.extremewinter.shelter.StarterShelter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import dev.extremewinter.temperature.TemperatureData;

public final class WinterClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        BlockPos[] shelterOrigin = new BlockPos[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var state = ShelterState.get(world);
                require(state.generated(), "new world generates starter shelter: " + state.outcome());
                shelterOrigin[0] = state.origin();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getBlockPos().equals(state.origin().add(4, 1, 7)), "first arrival is inside shelter");
                require(!Exposure.outdoors(world, player.getBlockPos()), "starter shelter has a protective roof");
                require(new HeatSources(ExtremeWinter.CONFIG.heatSourceRadius).strength(player) > 0,
                        "starter shelter provides heat at spawn");
                require(world.getBlockState(state.origin().add(6, 0, 6)).isOf(Blocks.WATER), "farm has a water source");
                require(world.getBlockState(state.origin().add(5, 1, 5)).isOf(Blocks.WHEAT), "farm has planted crops");
                var chest = (ChestBlockEntity) world.getBlockEntity(state.origin().add(1, 1, 3));
                require(chest != null && chest.getStack(0).getCount() == 8, "starter food is in template chest");
                chest.removeStack(0);
                chest.markDirty();
                new StarterShelter(ExtremeWinter.CONFIG).onStarted(server);
                require(chest.getStack(0).isEmpty(), "generation does not replenish loot");
            });
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("starter-shelter-interior");
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                var world = server.getOverworld();
                var environment = new WinterEnvironment(ExtremeWinter.CONFIG);
                BlockPos snow = new BlockPos(20, 100, 0);
                world.setBlockState(snow.down(), Blocks.STONE.getDefaultState());
                require(environment.trySnow(world, snow), "snow forms on exposed natural terrain");
                for (int i = 0; i < 12; i++) environment.trySnow(world, snow);
                require(world.getBlockState(snow).get(SnowBlock.LAYERS) == ExtremeWinter.CONFIG.maxSnowLayers,
                        "snow accumulation stops at configured cap");
                world.setBlockState(snow.up(3), Blocks.GLASS.getDefaultState());
                world.setBlockState(snow, Blocks.AIR.getDefaultState());
                require(!environment.trySnow(world, snow), "snow does not form under a glass roof");
                BlockPos crop = new BlockPos(20, 100, 3);
                world.setBlockState(crop.down(), Blocks.FARMLAND.getDefaultState());
                world.setBlockState(crop, Blocks.WHEAT.getDefaultState());
                require(!environment.trySnow(world, crop) && world.getBlockState(crop).isOf(Blocks.WHEAT),
                        "snow preserves crops");
                BlockPos machine = new BlockPos(20, 100, 6);
                world.setBlockState(machine.down(), Blocks.FURNACE.getDefaultState());
                require(!environment.trySnow(world, machine), "snow does not obstruct a block entity");
                BlockPos water = new BlockPos(23, 100, 0);
                world.setBlockState(water.down(), Blocks.STONE.getDefaultState());
                world.setBlockState(water, Blocks.WATER.getDefaultState());
                require(environment.tryFreeze(world, water) && world.getBlockState(water).isOf(Blocks.ICE),
                        "exposed source water freezes");
                world.setBlockState(water.up(3), Blocks.GLASS.getDefaultState());
                world.setBlockState(water, Blocks.WATER.getDefaultState());
                require(!environment.tryFreeze(world, water), "roofed water is protected from freezing");
                require(!environment.trySnow(world, new BlockPos(1000000, 100, 1000000)),
                        "unloaded columns are skipped");
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    world.setBlockState(new BlockPos(x, 99, z), Blocks.STONE.getDefaultState());
                }
                player.changeGameMode(GameMode.SURVIVAL);
                player.teleport(world, 0.5, 100, 0.5, Set.of(), 0, 0, true);
                world.setTimeOfDay(6000);
                world.setWeather(0, 6000, true, false);
                TemperatureData.set(player, 80);
                require(Exposure.outdoors(world, player.getBlockPos()), "open sky is exposed");
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(TemperatureData.get(player) < 80 && TemperatureData.get(player) > 79,
                        "outdoor temperature falls slowly");
                var world = server.getOverworld();
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    world.setBlockState(new BlockPos(x, 103, z), Blocks.GLASS.getDefaultState());
                }
                require(!Exposure.outdoors(world, player.getBlockPos()), "glass roof protects indoor space");
                world.setBlockState(new BlockPos(2, 100, 0), Blocks.CAMPFIRE.getDefaultState());
                var heat = new HeatSources(ExtremeWinter.CONFIG.heatSourceRadius);
                require(heat.strength(player) > 0, "lit campfire warms nearby player");
                for (int z = -4; z <= 4; z++) for (int y = 100; y <= 102; y++) {
                    world.setBlockState(new BlockPos(1, y, z), Blocks.STONE.getDefaultState());
                }
                require(heat.strength(player) == 0, "solid wall blocks heat");
                for (int z = -4; z <= 4; z++) for (int y = 100; y <= 102; y++) {
                    world.setBlockState(new BlockPos(1, y, z), Blocks.AIR.getDefaultState());
                }
                world.setBlockState(new BlockPos(2, 100, 0), Blocks.CAMPFIRE.getDefaultState().with(Properties.LIT, false));
                require(heat.strength(player) == 0, "extinguished campfire is not a heat source");
                world.setBlockState(new BlockPos(2, 100, 0), Blocks.CAMPFIRE.getDefaultState());
                TemperatureData.set(player, 30);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(TemperatureData.get(player) > 30, "heat restores temperature during real ticks");
                server.getOverworld().setBlockState(new BlockPos(2, 100, 0), Blocks.AIR.getDefaultState());
                TemperatureData.set(player, 10);
            });
            context.waitTicks(20);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.hasStatusEffect(StatusEffects.SLOWNESS), "severe hypothermia slows movement");
                require(player.hasStatusEffect(StatusEffects.MINING_FATIGUE), "severe hypothermia slows mining");
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    server.getOverworld().setBlockState(new BlockPos(x, 103, z), Blocks.AIR.getDefaultState());
                }
                player.setHealth(20);
                player.getHungerManager().setFoodLevel(16);
                TemperatureData.set(player, 0);
            });
            context.waitTicks(120);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getHealth() < 20, "minimum temperature causes periodic freezing damage");
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    server.getOverworld().setBlockState(new BlockPos(x, 103, z), Blocks.GLASS.getDefaultState());
                }
                TemperatureData.set(player, 80);
                player.setHealth(20);
            });
            context.waitTicks(60);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(!player.hasStatusEffect(StatusEffects.SLOWNESS), "cold slowness expires after warming");
                require(!player.hasStatusEffect(StatusEffects.MINING_FATIGUE), "cold fatigue expires after warming");
                require(TemperatureData.get(player) == 80, "shelter stabilizes warm temperature");
            });
            context.runOnClient(client -> require(TemperatureHud.current() != null
                    && TemperatureHud.current().value() == 80, "HUD receives the server's temperature"));
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("phase6-temperature-hud");
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var state = ShelterState.get(server.getOverworld());
                require(state.generated() && state.origin().equals(shelterOrigin[0]), "shelter marker survives reload");
                var chest = (ChestBlockEntity) server.getOverworld().getBlockEntity(state.origin().add(1, 1, 3));
                require(chest != null && chest.getStack(0).isEmpty(), "reload does not replenish starter chest");
                require(server.getPlayerManager().getPlayerList().getFirst().getBlockY() == 100,
                        "rejoin does not teleport player back to shelter");
            });
            game.getServer().runOnServer(server -> require(
                    TemperatureData.get(server.getPlayerManager().getPlayerList().getFirst()) == 80,
                    "temperature survives save and rejoin"));
            context.waitTicks(20);
            context.runOnClient(client -> require(TemperatureHud.current() != null
                    && TemperatureHud.current().value() == 80, "HUD resynchronizes after rejoin"));
        }
        context.runOnClient(client -> require(TemperatureHud.current() == null, "HUD resets on disconnect"));
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
