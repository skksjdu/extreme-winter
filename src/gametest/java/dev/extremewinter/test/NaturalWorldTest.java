package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.temperature.HeatSources;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameMode;
import net.minecraft.block.Blocks;

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
            creator.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, null);
        }).create()) {
            game.getClientWorld().waitForChunksRender();
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getInventory().count(net.minecraft.item.Items.CAMPFIRE) == 1,
                        "natural world receives one campfire");
                require(world.getStructureTemplateManager().getTemplate(
                        net.minecraft.util.Identifier.of(ExtremeWinter.ID, "starter_shelter")).isEmpty(),
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
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.changeGameMode(GameMode.CREATIVE);
                player.getAbilities().flying = true;
                player.sendAbilitiesUpdate();
                var view = world.getSpawnPos().add(18, 15, -20);
                player.teleport(world, view.getX() + 0.5, view.getY(), view.getZ() + 0.5,
                        Set.of(), 30, 18, true);
            });
            context.waitTicks(30);
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("natural-terrain-" + profile);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                world.setTimeOfDay(6000);
                world.setWeather(6000, 0, false, false);
                for (int x = -2; x <= 12; x++) for (int z = -2; z <= 5; z++) {
                    world.setBlockState(new net.minecraft.util.math.BlockPos(x, 139, z), Blocks.STONE.getDefaultState());
                }
                world.setBlockState(new net.minecraft.util.math.BlockPos(1, 140, 0), Blocks.SNOW_BLOCK.getDefaultState());
                world.setBlockState(new net.minecraft.util.math.BlockPos(3, 140, 0), dev.extremewinter.environment.WinterBlocks.SNOW_DRIFT
                        .getDefaultState().with(net.minecraft.block.SnowBlock.LAYERS, 8));
                world.setBlockState(new net.minecraft.util.math.BlockPos(5, 140, 0), Blocks.SPRUCE_PLANKS.getDefaultState());
                world.setBlockState(new net.minecraft.util.math.BlockPos(7, 140, 0), Blocks.OAK_LEAVES.getDefaultState()
                        .with(net.minecraft.state.property.Properties.PERSISTENT, true));
                world.setBlockState(new net.minecraft.util.math.BlockPos(9, 140, 0), Blocks.SPRUCE_LEAVES.getDefaultState()
                        .with(net.minecraft.state.property.Properties.PERSISTENT, true));
                world.setBlockState(new net.minecraft.util.math.BlockPos(3, 140, 3), dev.extremewinter.environment.WinterBlocks.SNOW_DRIFT
                        .getDefaultState().with(net.minecraft.block.SnowBlock.LAYERS, 3));
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.teleport(world, 5.5, 142, -7, Set.of(), 0, 17, true);
            });
            context.waitTicks(100);
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("vanilla-snow-leaves-" + profile);
            ExtremeWinter.LOGGER.info("TEST {}: natural world and rendering checks PASSED", profile);
        }
    }

    private static void require(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
