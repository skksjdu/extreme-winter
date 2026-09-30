package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.environment.WinterEnvironment;
import dev.extremewinter.environment.WinterBlocks;
import dev.extremewinter.environment.SnowDriftBlock;
import dev.extremewinter.temperature.HeatSources;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
        float[] mildDamage = new float[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getClientWorld().waitForChunksRender();
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getInventory().count(Items.CAMPFIRE) == 1, "new save receives exactly one campfire");
                dev.extremewinter.survival.StarterSupplies.onJoin(player);
                require(player.getInventory().count(Items.CAMPFIRE) == 1, "starter supply is not duplicated");
            });
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                var world = server.getOverworld();
                var environment = new WinterEnvironment(ExtremeWinter.CONFIG);
                BlockPos snow = new BlockPos(20, 100, 0);
                world.setBlockState(snow.down(), Blocks.STONE.getDefaultState());
                require(environment.trySnow(world, snow), "snow forms on exposed natural terrain");
                for (int i = 0; i < ExtremeWinter.CONFIG.maxSnowLayers + 5; i++) environment.trySnow(world, snow);
                int total = 0;
                for (int y = 0; y < 10; y++) total += SnowDriftBlock.layers(world.getBlockState(snow.up(y)));
                require(total == ExtremeWinter.CONFIG.maxSnowLayers && total > 8,
                        "snow stacks across blocks and stops at configured column cap");
                for (int y = 0; y < 10; y++) world.setBlockState(snow.up(y), Blocks.AIR.getDefaultState());
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
                BlockPos drift = new BlockPos(25, 100, 8);
                world.setBlockState(drift.down(), Blocks.STONE.getDefaultState());
                var shovel = new ItemStack(Items.STONE_SHOVEL);
                var fullSnow = WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, 8);
                player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, shovel);
                require(player.canHarvest(fullSnow), "ordinary shovel can harvest gravity snow");
                var loot = Block.getDroppedStacks(fullSnow, world, drift, null, player, shovel);
                require(loot.stream().filter(stack -> stack.isOf(Items.SNOWBALL)).mapToInt(ItemStack::getCount).sum() == 8,
                        "ordinary shovel yields one snowball per snow layer");
                player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
                for (int y = 0; y < 3; y++) world.setBlockState(drift.up(y),
                        WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, 8));
                world.breakBlock(drift, false);
                BlockPos thin = new BlockPos(28, 100, 8);
                world.setBlockState(thin.down(), Blocks.STONE.getDefaultState());
                world.setBlockState(thin, WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, 5));
                world.setBlockState(thin.up(3), WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, 7));
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
                var testWorld = server.getOverworld();
                BlockPos drift = new BlockPos(25, 100, 8);
                require(SnowDriftBlock.layers(testWorld.getBlockState(drift)) == 8
                        && SnowDriftBlock.layers(testWorld.getBlockState(drift.up())) == 8
                        && testWorld.getBlockState(drift.up(2)).isAir(), "upper snow falls after removing the bottom: "
                        + testWorld.getBlockState(drift) + " / " + testWorld.getBlockState(drift.up())
                        + " / " + testWorld.getBlockState(drift.up(2)));
                BlockPos thin = new BlockPos(28, 100, 8);
                require(SnowDriftBlock.layers(testWorld.getBlockState(thin)) == 8
                        && SnowDriftBlock.layers(testWorld.getBlockState(thin.up())) == 4,
                        "falling thin snow merges without losing layers");
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(TemperatureData.get(player) < 80 && TemperatureData.get(player) >= 78,
                        "outdoor temperature falls at the harsher rate");
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
                TemperatureData.set(player, 35);
            });
            context.waitTicks(100);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                mildDamage[0] = 20 - player.getHealth();
                require(mildDamage[0] > 0 && mildDamage[0] < 5, "temperature below 40 causes gradual damage");
                player.setHealth(20);
                TemperatureData.set(player, 0);
            });
            context.waitTicks(120);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getHealth() < 20, "minimum temperature causes periodic freezing damage");
                require(20 - player.getHealth() > mildDamage[0], "colder temperature causes more damage");
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
                    && TemperatureHud.current().value() == 80
                    && TemperatureHud.halfIcons(TemperatureHud.current()) == 16,
                    "HUD receives temperature and maps it to eight full warmth flames"));
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("phase6-temperature-hud");
            context.runOnClient(client -> TemperatureHud.update(new dev.extremewinter.network.TemperaturePayload(85, 0, 100, 0)));
            context.takeScreenshot("warmth-85-left-to-right");
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                // Contain the HUD fixture so water cannot spread beyond the later cleanup volume.
                for (int y = 100; y <= 102; y++) for (int edge = -2; edge <= 2; edge++) {
                    world.setBlockState(new BlockPos(-2, y, edge), Blocks.GLASS.getDefaultState());
                    world.setBlockState(new BlockPos(2, y, edge), Blocks.GLASS.getDefaultState());
                    world.setBlockState(new BlockPos(edge, y, -2), Blocks.GLASS.getDefaultState());
                    world.setBlockState(new BlockPos(edge, y, 2), Blocks.GLASS.getDefaultState());
                }
                for (int y = 100; y <= 102; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.WATER.getDefaultState());
                }
                TemperatureData.set(server.getPlayerManager().getPlayerList().getFirst(), 55);
            });
            context.waitTicks(20);
            context.runOnClient(client -> require(client.player.isSubmergedInWater(), "underwater HUD fixture is submerged"));
            context.takeScreenshot("underwater-warmth-hud");
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                for (int y = 100; y <= 102; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
                TemperatureData.set(server.getPlayerManager().getPlayerList().getFirst(), 80);
            });
            context.waitTicks(20);
            context.runOnClient(client -> require(!client.player.isTouchingWater(), "HUD fixture water is fully removed"));
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getBlockY() == 100, "rejoin keeps saved player position");
                require(player.getInventory().count(Items.CAMPFIRE) == 0, "rejoin does not replenish used starter supply");
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
