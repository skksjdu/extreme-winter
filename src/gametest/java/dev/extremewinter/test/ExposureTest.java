package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.environment.WinterBlocks;
import dev.extremewinter.temperature.HeatItems;
import dev.extremewinter.temperature.HeatWeathering;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ExposureTest implements FabricClientGameTest {
    static void roof(ServerLevel world, BlockPos center, BlockState state) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) world.setBlock(center.offset(x, 0, z), state, 3);
    }
    static void weather(ServerLevel world, int clear, int rain, boolean raining, boolean thunder) {
        var data = world.getWeatherData();
        data.setClearWeatherTime(clear);
        data.setRainTime(rain);
        data.setThunderTime(rain);
        data.setRaining(raining);
        data.setThundering(thunder);
    }
    static void time(ServerLevel world, long value) {
        var clock = world.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        world.clockManager().setTotalTicks(clock, value);
    }
    @Override public void runTest(ClientGameTestContext context) {
        var pos = new BlockPos(4, 110, 4);
        var top = pos.above(3);
        try (var game = context.worldBuilder().create()) {
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                for (int x = 0; x <= 12; x++) for (int z = 0; z <= 8; z++) world.setBlock(new BlockPos(x, 109, z), Blocks.STONE.defaultBlockState(), 3);
                require(Exposure.outdoors(world, pos.above()), "open sky is exposed");
                for (var block : new Block[]{Blocks.STONE, Blocks.GLASS, Blocks.OAK_LEAVES}) {
                    world.setBlock(top, block.defaultBlockState(), 3);
                    require(Exposure.outdoors(world, pos.above()), "one top block never creates shelter");
                    world.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
                }
                for (int x = -1; x <= 1; x++) world.setBlock(top.offset(x, 0, 0), Blocks.STONE.defaultBlockState(), 3);
                require(Exposure.outdoors(world, pos.above()), "one-block-wide beam is exposed");
                roof(world, top, Blocks.GLASS.defaultBlockState());
                require(!Exposure.outdoors(world, pos.above()), "3x3 glass roof shelters its center");
                require(Exposure.outdoors(world, pos.above().east()), "roof edge is exposed");
                world.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
                require(Exposure.outdoors(world, pos.above()), "opening directly overhead stays exposed");
                world.setBlock(top, Blocks.GLASS.defaultBlockState(), 3);
                world.setBlock(top.offset(-1, 0, -1), Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(top.offset(1, 0, 1), Blocks.AIR.defaultBlockState(), 3);
                require(!Exposure.outdoors(world, pos.above()), "seven covered columns are enough");
                world.setBlock(top.offset(1, 0, -1), Blocks.AIR.defaultBlockState(), 3);
                require(Exposure.outdoors(world, pos.above()), "six covered columns are insufficient");
                roof(world, top, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
                require(!Exposure.outdoors(world, pos.above()), "broad foliage shelters");
                roof(world, top, Blocks.STONE.defaultBlockState());
                require(!Exposure.outdoors(world, pos.above()), "cave ceiling shelters");
                roof(world, top, Blocks.AIR.defaultBlockState());
                world.setBlock(pos, WinterBlocks.SNOW_DRIFT.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8), 3);
                require(Exposure.outdoors(world, pos.above()), "snow around the feet is not a roof");
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                var missing = new BlockPos(1000000, 110, 1000000);
                require(Exposure.outdoors(world, missing) && world.getChunkSource().getChunkNow(missing.getX() >> 4, missing.getZ() >> 4) == null, "missing chunks stay unloaded");
                var camp = new BlockPos(10, 110, 4);
                world.setBlock(camp, Blocks.CAMPFIRE.defaultBlockState(), 3);
                world.setBlock(camp.above(3), Blocks.GLASS.defaultBlockState(), 3);
                var wear = new HeatWeathering(ExtremeWinter.CONFIG);
                wear.advanceBlockEntity(world, world.getBlockEntity(camp));
                require(HeatItems.elapsed(world.getBlockEntity(camp)) == 1, "one top block does not pause campfire wear");
                roof(world, camp.above(3), Blocks.GLASS.defaultBlockState());
                wear.advanceBlockEntity(world, world.getBlockEntity(camp));
                require(HeatItems.elapsed(world.getBlockEntity(camp)) == 1, "real roof pauses campfire wear");
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(world, 4.5, 110, 4.5, Set.of(), 0, 0, true);
                world.setBlock(top, Blocks.GLASS.defaultBlockState(), 3);
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                require(TemperatureData.get(player) < 80, "player still cools under a single top block");
                roof(server.overworld(), top, Blocks.GLASS.defaultBlockState());
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> require(TemperatureData.get(server.getPlayerList().getPlayers().getFirst()) == 80, "actual player ticks stay warm under a proper roof"));
        }
        ExtremeWinter.LOGGER.info("TEST 3x3 shelter, lone blocks, beams, roof edges, gaps, canopy, cave, snow and real heat/player ticks PASSED");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
