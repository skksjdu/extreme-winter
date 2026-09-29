package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.shelter.ShelterState;
import dev.extremewinter.temperature.HeatSources;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameMode;
import net.minecraft.block.Blocks;
import net.minecraft.world.LightType;

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
                var shelter = ShelterState.get(world);
                require(shelter.generated(), "natural terrain shelter: " + shelter.outcome());
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(!Exposure.outdoors(world, player.getBlockPos()), "natural terrain spawn is sheltered");
                var heat = new HeatSources(ExtremeWinter.CONFIG.heatSourceRadius);
                require(heat.strength(player) > 0, "natural terrain spawn has heat");
                for (int x = 5; x <= 7; x++) for (int z = 5; z <= 8; z++) {
                    if (x == 6 && z == 6) continue;
                    var crop = shelter.origin().add(x, 1, z);
                    require(world.getBlockState(crop).isOf(Blocks.WHEAT), "farm crops survive ticks");
                    require(world.getLightLevel(LightType.BLOCK, crop) >= 9, "farm can grow without daylight");
                }
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
            context.takeScreenshot("natural-shelter-interior-" + profile);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var origin = ShelterState.get(world).origin();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.changeGameMode(GameMode.CREATIVE);
                player.getAbilities().flying = true;
                player.sendAbilitiesUpdate();
                player.teleport(world, origin.getX() + 15, origin.getY() + 9, origin.getZ() - 13,
                        Set.of(), 32, 18, true);
            });
            context.waitTicks(30);
            game.getClientWorld().waitForChunksRender();
            context.takeScreenshot("natural-shelter-exterior-" + profile);
            ExtremeWinter.LOGGER.info("TEST {}: natural world and rendering checks PASSED", profile);
        }
    }

    private static void require(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
