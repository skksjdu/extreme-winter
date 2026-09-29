package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.WinterBlocks;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/** Real client input checks sinking, walking speed, jumping and recovery outside snow. */
public final class SnowMovementTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (String species : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                    "mangrove", "cherry", "pale_oak", "azalea", "flowering_azalea"}) {
                for (String asset : new String[]{"models/block/" + species + "_leaves.json",
                        "textures/block/" + species + "_leaves.png"}) {
                    var resource = client.getResourceManager().getResource(Identifier.ofVanilla(asset)).orElseThrow();
                    // Sodium supplies its own mipmap-safe copies of vanilla leaf textures.
                    String expected = asset.startsWith("textures/") && FabricLoader.getInstance().isModLoaded("sodium")
                            ? "sodium" : "vanilla";
                    require(resource.getPackId().equals(expected),
                            "leaf resource: " + asset + " from " + resource.getPackId() + " expected " + expected);
                }
            }
        });
        try (var game = context.worldBuilder().create()) {
            game.getClientWorld().waitForChunksRender();
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                for (int x = 98; x <= 135; x++) for (int z = -2; z <= 20; z++) {
                    world.setBlockState(new BlockPos(x, 99, z), Blocks.STONE.getDefaultState());
                    world.setBlockState(new BlockPos(x, 104, z), Blocks.GLASS.getDefaultState());
                }
                for (int x = 98; x <= 135; x++) for (int offset = -1; offset <= 1; offset++) {
                    for (int z : new int[]{6, 12, 18}) {
                        var snow = WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, z == 12 ? 2 : 8);
                        world.setBlockState(new BlockPos(x, 100, z + offset), snow);
                        if (z == 18) world.setBlockState(new BlockPos(x, 101, z + offset), snow);
                    }
                }
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.changeGameMode(GameMode.SURVIVAL);
                TemperatureData.set(player, 80);
                for (int layers = 1; layers <= 8; layers++) {
                    var snow = WinterBlocks.SNOW_DRIFT.getDefaultState().with(SnowBlock.LAYERS, layers);
                    var pos = new BlockPos(100, 100, 6);
                    require(snow.getCollisionShape(world, pos, ShapeContext.of(player)).getMax(Direction.Axis.Y)
                            == layers / 16.0, "player compresses " + layers + " snow layers");
                    require(snow.getCollisionShape(world, pos).getMax(Direction.Axis.Y)
                            == layers / 8.0, "non-player collision preserves snow support");
                }
            });
            double[] distance = new double[4];
            double groundJump = 0;
            int[] lanes = {0, 6, 12, 18};
            double[] heights = {100, 100.5, 100.125, 101.5};
            for (int lane = 0; lane < lanes.length; lane++) {
                int z = lanes[lane];
                double height = heights[lane];
                game.getServer().runOnServer(server -> {
                    var player = server.getPlayerManager().getPlayerList().getFirst();
                    player.setVelocity(Vec3d.ZERO);
                    player.teleport(server.getOverworld(), 100.5, height + 0.7, z + 0.5,
                            Set.of(), -90, 0, true);
                });
                game.getClientWorld().waitForChunksRender();
                context.waitFor(client -> Math.abs(client.player.getY() - height) < 0.001 && client.player.isOnGround());
                context.waitTicks(20);
                context.runOnClient(client -> {
                    require(Math.abs(client.player.getY() - height) < 0.001,
                            "client settles inside lane " + z + ": " + client.player.getY() + " expected " + height);
                    require(client.player.isOnGround(), "snow supports player in lane " + z);
                });
                game.getServer().runOnServer(server -> require(Math.abs(
                        server.getPlayerManager().getPlayerList().getFirst().getY() - height) < 0.001,
                        "server agrees on sinking in lane " + z));
                if (lane == 0) groundJump = jumpHeight(context);
                double start = context.computeOnClient(client -> client.player.getX());
                context.getInput().holdKeyFor(options -> options.forwardKey, 30);
                distance[lane] = context.computeOnClient(client -> client.player.getX()) - start;
            }
            double ratio = distance[1] / distance[0];
            require(distance[0] > 5 && ratio > 0.6 && ratio < 0.95,
                    "snow moderately slows real walking: " + distance[0] + " / " + distance[1]);
            require(Math.abs(distance[2] / distance[0] - ratio) < 0.03
                    && Math.abs(distance[3] / distance[0] - ratio) < 0.03,
                    "thin and stacked snow apply slowdown once");
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.setVelocity(Vec3d.ZERO);
                player.teleport(server.getOverworld(), 110.5, 100.5, 6.5, Set.of(), -90, 0, true);
            });
            context.waitTicks(20);
            double snowJump = jumpHeight(context);
            require(groundJump > 1 && Math.abs(snowJump - groundJump) < 0.01,
                    "snow preserves normal jump height: ground=" + groundJump + " snow=" + snowJump);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(player.getHealth() == 20, "shallow sinking causes no suffocation or freezing damage");
                player.setVelocity(Vec3d.ZERO);
                player.teleport(server.getOverworld(), 100.5, 100, 0.5, Set.of(), -90, 0, true);
            });
            context.waitTicks(20);
            double start = context.computeOnClient(client -> client.player.getX());
            context.getInput().holdKeyFor(options -> options.forwardKey, 30);
            double recovered = context.computeOnClient(client -> client.player.getX()) - start;
            require(Math.abs(recovered - distance[0]) < 0.05, "walking speed recovers outside snow");
            ExtremeWinter.LOGGER.info("TEST snow movement PASSED: ground={} snow={} ratio={} thin={} stacked={} jump={}/{}",
                    distance[0], distance[1], ratio, distance[2], distance[3], groundJump, snowJump);
        }
    }

    private static double jumpHeight(ClientGameTestContext context) {
        double start = context.computeOnClient(client -> client.player.getY());
        double highest = start;
        context.getInput().holdKey(options -> options.jumpKey);
        for (int tick = 0; tick < 12; tick++) {
            context.waitTick();
            highest = Math.max(highest, context.computeOnClient(client -> client.player.getY()));
        }
        context.getInput().releaseKey(options -> options.jumpKey);
        context.waitTicks(30);
        return highest - start;
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
