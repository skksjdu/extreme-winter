package dev.extremewinter.test;

import net.minecraft.world.level.gamerules.GameRules;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.temperature.HeatSources;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** A real terrain world, independent of the controlled flat-world regression fixtures. */
public final class NaturalWorldTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        String profile = System.getProperty("winter.test.profile", "A");
        require(FabricLoader.getInstance().isModLoaded("sodium") == !profile.equals("A"), "expected Sodium profile");
        require(FabricLoader.getInstance().isModLoaded("iris") == "CDE".contains(profile), "expected Iris profile");
        if (profile.equals("E")) {
            for (String id : new String[]{"lithium", "ferritecore", "modmenu"}) {
                require(FabricLoader.getInstance().isModLoaded(id), "expected " + id);
            }
        }
        try (var game = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(creator -> {
            creator.setSeed("20260929");
            creator.getGameRules().set(GameRules.SPAWN_MOBS, false, null);
        }).create()) {
            game.getClientLevel().waitForChunksRender();
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                require(player.getInventory().countItem(net.minecraft.world.item.Items.CAMPFIRE) == 1,
                        "natural world receives one campfire");
                require(world.getStructureManager().get(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "starter_shelter")).isEmpty(),
                        "shelter template is removed");
                var heat = new HeatSources(ExtremeWinter.CONFIG.heatSourceRadius);
                require(heat.strength(player) == 0, "carried campfire does not warm the player");
                long start = System.nanoTime();
                for (int i = 0; i < 100; i++) heat.strength(player);
                ExtremeWinter.LOGGER.info("TEST {}: heat scan mean {} microseconds (100 warm calls)",
                        profile, (System.nanoTime() - start) / 100000.0);
            });
            context.runOnClient(client -> {
                require(TemperatureHud.current() != null, "natural world HUD receives temperature");
                if (FabricLoader.getInstance().isModLoaded("iris")) {
                    try {
                        Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                        Object iris = api.getMethod("getInstance").invoke(null);
                        boolean active = (boolean) api.getMethod("isShaderPackInUse").invoke(iris);
                        require(active == profile.equals("D"), "shader activation matches profile " + profile);
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError("Could not verify Iris public API", exception);
                    }
                }
            });
            context.takeScreenshot("natural-spawn-" + profile);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                var view = world.getRespawnData().pos().offset(18, 15, -20);
                player.teleportTo(world, view.getX() + 0.5, view.getY(), view.getZ() + 0.5,
                        Set.of(), 30, 18, true);
            });
            context.waitTicks(30);
            game.getClientLevel().waitForChunksRender();
            context.takeScreenshot("natural-terrain-" + profile);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                ExposureTest.time(world, 6000);
                ExposureTest.weather(world, 6000, 0, false, false);
                for (int x = -2; x <= 12; x++) for (int z = -2; z <= 5; z++) {
                    world.setBlock(new net.minecraft.core.BlockPos(x, 139, z), Blocks.STONE.defaultBlockState(), 3);
                }
                world.setBlock(new net.minecraft.core.BlockPos(1, 140, 0), Blocks.SNOW_BLOCK.defaultBlockState(), 3);
                world.setBlock(new net.minecraft.core.BlockPos(3, 140, 0), dev.extremewinter.environment.WinterBlocks.SNOW_DRIFT
                        .defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 8), 3);
                world.setBlock(new net.minecraft.core.BlockPos(5, 140, 0), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
                world.setBlock(new net.minecraft.core.BlockPos(7, 140, 0), Blocks.OAK_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT, true), 3);
                world.setBlock(new net.minecraft.core.BlockPos(9, 140, 0), Blocks.SPRUCE_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT, true), 3);
                world.setBlock(new net.minecraft.core.BlockPos(3, 140, 3), dev.extremewinter.environment.WinterBlocks.SNOW_DRIFT
                        .defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 3), 3);
                var player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(world, 5.5, 142, -7, Set.of(), 0, 17, true);
            });
            context.waitTicks(100);
            game.getClientLevel().waitForChunksRender();
            context.takeScreenshot("vanilla-snow-leaves-" + profile);
            ExtremeWinter.LOGGER.info("TEST {}: natural world and rendering checks PASSED", profile);
        }
    }

    private static void require(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
