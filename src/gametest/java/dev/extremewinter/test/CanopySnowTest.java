package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.CanopySnowflakes;
import dev.extremewinter.environment.CanopySnow;
import dev.extremewinter.environment.SnowDriftBlock;
import dev.extremewinter.environment.WinterBlocks;
import dev.extremewinter.environment.WinterEnvironment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CanopySnowTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        int radius = ExtremeWinter.CONFIG.simulationRadiusChunks;
        int samples = ExtremeWinter.CONFIG.samplesPerPass;
        var origin = new BlockPos(8, 110, 8);
        int[] wait = new int[1];
        try (var game = context.worldBuilder().create()) {
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                ExtremeWinter.CONFIG.simulationRadiusChunks = 0;
                ExtremeWinter.CONFIG.samplesPerPass = 64;
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    world.setBlock(new BlockPos(x, 109, z), Blocks.STONE.defaultBlockState(), 3);
                    world.setBlock(new BlockPos(x, 125, z), Blocks.OAK_LEAVES.defaultBlockState()
                            .setValue(BlockStateProperties.PERSISTENT, true), 3);
                }
                var snow = new WinterEnvironment(ExtremeWinter.CONFIG);
                require(CanopySnow.belowLeaves(world, origin), "foliage permits sparse snow");
                require(snow.trySnow(world, origin) && snow.trySnow(world, origin)
                        && snow.trySnow(world, origin), "canopy snow grows past two layers");
                world.setBlock(origin.above(16), WinterBlocks.SNOW_DRIFT.defaultBlockState()
                        .setValue(SnowLayerBlock.LAYERS, 8), 3);
                require(origin.equals(CanopySnow.floorBelowLeaves(world, origin.getX(), origin.getZ()))
                        && CanopySnow.belowLeaves(world, origin), "snow on foliage does not hide the forest floor");
                world.setBlock(origin, WinterBlocks.SNOW_DRIFT.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4), 3);
                require(snow.trySnow(world, origin) && SnowDriftBlock.layers(world.getBlockState(origin)) == 5,
                        "existing deeper snow continues growing");
                var unlimited = new WinterEnvironment(new dev.extremewinter.config.WinterConfig());
                for (int i = 0; i < 91; i++) require(unlimited.trySnow(world, origin), "canopy snow keeps growing beyond the old cap");
                int deep = 0;
                for (int y = 0; y < 12; y++) deep += SnowDriftBlock.layers(world.getBlockState(origin.above(y)));
                require(deep == 96, "canopy snow crosses block boundaries without an artificial thickness limit");
                for (int y = 1; y < 15; y++) world.setBlock(origin.above(y), Blocks.AIR.defaultBlockState(), 3);
                for (var roof : new net.minecraft.world.level.block.Block[]{Blocks.GLASS, Blocks.STONE}) {
                    world.setBlock(origin.above(3), roof.defaultBlockState(), 3);
                    require(!CanopySnow.belowLeaves(world, origin) && !snow.trySnow(world, origin),
                            "solid roofs below foliage still block snow");
                }
                world.setBlock(origin.above(3), Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(origin, Blocks.WHEAT.defaultBlockState(), 3);
                require(!snow.trySnow(world, origin), "foliage snow preserves crops");
                world.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(origin.below(), Blocks.FURNACE.defaultBlockState(), 3);
                require(!snow.trySnow(world, origin), "foliage snow preserves machines");
                world.setBlock(origin, Blocks.WATER.defaultBlockState(), 3);
                require(!snow.tryFreeze(world, origin), "foliage does not relax the freezing rule");
                var missing = new BlockPos(1000000, 110, 1000000);
                require(!CanopySnow.belowLeaves(world, missing)
                        && CanopySnow.floorBelowLeaves(world, missing.getX(), missing.getZ()) == null
                        && world.getChunkSource().getChunkNow(missing.getX() >> 4, missing.getZ() >> 4) == null,
                        "canopy scans never load missing chunks");
                world.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
                world.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                server.getPlayerList().getPlayers().getFirst().teleportTo(world, 8.5, 110, 8.5, Set.of(), 0, 0, true);
                ExposureTest.time(world, 6000);
                ExposureTest.weather(world, 0, 6000, true, false);
                wait[0] = (int) (80 - world.getGameTime() % 80) + 2;
            });
            context.waitTicks(wait[0]);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 110; y < 125; y++)
                    world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> require(layers(server.overworld()) == 0,
                    "canopy sampling skips the ordinary 20/40/60-tick passes"));
            context.waitTicks(45);
            game.getServer().runOnServer(server -> require(layers(server.overworld()) > 0,
                    "real environment ticks accumulate snow on the fourth pass"));
            game.getClientLevel().waitForChunksRender();
            context.runOnClient(client -> {
                client.level.setRainLevel(1);
                int count = CanopySnowflakes.emit(client.level, origin, RandomSource.create(42));
                require(count > 0 && count <= 4, "foliage spawns a bounded batch of native snowflake particles");
                client.level.setRainLevel(0);
                require(CanopySnowflakes.emit(client.level, origin, RandomSource.create(42)) == 0,
                        "clear weather creates no extra flakes");
                client.level.setRainLevel(1);
            });
            context.waitTicks(20);
            context.runOnClient(client -> require(Integer.parseInt(client.particleEngine.countParticles()) > 0,
                    "native particles actually enter the client particle engine"));
            context.takeScreenshot("canopy-snow-26.0.1-" + System.getProperty("winter.test.profile", "A"));
            game.getServer().runOnServer(server -> {
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                    server.overworld().setBlock(new BlockPos(x, 114, z), Blocks.GLASS.defaultBlockState(), 3);
            });
            context.waitTicks(20);
            context.runOnClient(client -> {
                client.level.setRainLevel(1);
                require(CanopySnowflakes.emit(client.level, origin, RandomSource.create(42)) == 0,
                        "a glass ceiling underneath leaves blocks all additional flakes");
            });
        } finally {
            ExtremeWinter.CONFIG.simulationRadiusChunks = radius;
            ExtremeWinter.CONFIG.samplesPerPass = samples;
        }
        ExtremeWinter.LOGGER.info("TEST canopy snow, quarter-rate real sampling, uncapped thickness, roof protection and native flakes PASSED");
    }

    private static int layers(ServerLevel world) {
        int total = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = 110; y < 125; y++) total += SnowDriftBlock.layers(world.getBlockState(new BlockPos(x, y, z)));
        }
        return total;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
