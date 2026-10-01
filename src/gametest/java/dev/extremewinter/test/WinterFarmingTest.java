package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public final class WinterFarmingTest implements FabricClientGameTest {
    private static final BlockPos CROP = new BlockPos(8, 100, 8), HEAT = CROP.east(4);
    private static void require(boolean b, String m) { if (!b) throw new AssertionError(m); }
    private static int attempts(net.minecraft.server.level.ServerLevel w, BlockState initial, int count, long seed) {
        var random = RandomSource.create(seed); int grown = 0;
        for (int trial = 0; trial < count; trial++) {
            w.setBlock(CROP, initial, 2);
            initial.randomTick(w, CROP, random);
            if (!w.getBlockState(CROP).equals(initial)) grown++;
        }
        return grown;
    }
    public void runTest(ClientGameTestContext context) {
        try (var game = context.worldBuilder().create()) {
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.CREATIVE); p.teleportTo(w, 5.5, 100, 8.5, Set.of(), 0, 0, true);
                w.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, server);
                w.getGameRules().set(GameRules.ADVANCE_TIME, false, server); ExposureTest.time(w, 6000);
                for (int x = 4; x <= 16; x++) for (int z = 4; z <= 12; z++) w.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                for (int x = 7; x <= 9; x++) for (int z = 7; z <= 9; z++) w.setBlock(new BlockPos(x, 99, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 3);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var clock = WinterWorldState.get(w); var wheat = Blocks.WHEAT.defaultBlockState();
                require(w.getRawBrightness(CROP, 0) >= 9, "daytime light reached crop");
                clock.setElapsedTicks(180 * 1200 - 100); int normal = attempts(w, wheat, 12000, 10);
                clock.setElapsedTicks(180 * 1200); int deep = attempts(w, wheat, 12000, 11);
                clock.setElapsedTicks(270 * 1200); int winter = attempts(w, wheat, 12000, 12);
                double half = (double) deep / normal, quarter = (double) winter / normal;
                require(normal > 300 && half > .40 && half < .60 && quarter > .18 && quarter < .32,
                        "actual wheat random growth ratios about one/half/quarter: " + normal + "/" + deep + "/" + winter);
                ExtremeWinter.LOGGER.info("TEST D native 12000 wheat attempts per phase normal={} deep={} long={} ratios={}/{}", normal, deep, winter, half, quarter);
                for (var block : new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.SWEET_BERRY_BUSH}) {
                    require(block.defaultBlockState().is(WinterFarming.CROPS), "selected native crop belongs to tag: " + block);
                    w.setBlock(CROP.below(), block == Blocks.SWEET_BERRY_BUSH ? Blocks.DIRT.defaultBlockState() : Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 3);
                    clock.setElapsedTicks(170 * 1200); int a = attempts(w, block.defaultBlockState(), 6000, 100);
                    clock.setElapsedTicks(270 * 1200); int b = attempts(w, block.defaultBlockState(), 6000, 101);
                    require(a > 50 && b > 0 && (double) b / a > .12 && (double) b / a < .38, "each tagged native crop still grows more slowly: " + block + " " + a + "/" + b);
                    ExtremeWinter.LOGGER.info("TEST D tagged crop={} normal={} long={}", block, a, b);
                }
                w.setBlock(CROP.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 3);
                w.setBlock(CROP, wheat, 3); var bone = new ItemStack(Items.BONE_MEAL);
                require(BoneMealItem.growCrop(bone, w, CROP) && ((CropBlock) Blocks.WHEAT).getAge(w.getBlockState(CROP)) > 0 && bone.isEmpty(), "real bone meal works and consumes normally in outdoor long winter");
                require(WinterFarming.allowGrowth(Blocks.OAK_SAPLING.defaultBlockState(), w, CROP, RandomSource.create(1)), "tree sapling never enters crop gate");
                var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
                require(WinterFarming.allowGrowth(wheat, nether, CROP, RandomSource.create(2)), "other dimensions never enter crop gate");
                for (int x = 6; x <= 14; x++) for (int z = 6; z <= 10; z++) w.setBlock(new BlockPos(x, 104, z), Blocks.GLASS.defaultBlockState(), 3);
                w.setBlock(HEAT, Blocks.CAMPFIRE.defaultBlockState(), 3);
                w.setBlock(CROP, wheat, 3);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var heat = new HeatSources(ExtremeWinter.CONFIG);
                require(Math.abs(heat.strengthAt(w, CROP) - .25) < 1e-9 && WinterFarming.greenhouse(w, CROP), "roof/light/visible lit campfire exactly meet heat threshold .25");
                int warm = attempts(w, Blocks.WHEAT.defaultBlockState(), 12000, 13);
                WinterWorldState.get(w).setElapsedTicks(170 * 1200); int normal = attempts(w, Blocks.WHEAT.defaultBlockState(), 12000, 14);
                WinterWorldState.get(w).setElapsedTicks(270 * 1200);
                require((double) warm / normal > .85 && (double) warm / normal < 1.15, "greenhouse actually restores native growth ratio: " + warm + "/" + normal);
                ExtremeWinter.LOGGER.info("TEST D native greenhouse attempts warm={} normal={} heat={} light={}", warm, normal, heat.strengthAt(w, CROP), w.getRawBrightness(CROP, 0));
                w.setBlock(CROP.east(2), Blocks.STONE.defaultBlockState(), 3);
            });
            context.waitTicks(41);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); require(!WinterFarming.greenhouse(w, CROP), "wall occlusion updates within two seconds");
                w.removeBlock(CROP.east(2), false);
            });
            context.waitTicks(41);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); require(WinterFarming.greenhouse(w, CROP), "removing wall restores greenhouse within two seconds");
                w.removeBlock(HEAT, false);
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, CROP) == 0, "removing campfire invalidates heat immediately");
                w.setBlock(HEAT, Blocks.FURNACE.defaultBlockState(), 3);
                var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) w.getBlockEntity(HEAT);
                furnace.setItem(0, new ItemStack(Items.OAK_LOG, 2)); furnace.setItem(1, new ItemStack(Items.COAL));
            });
            context.waitTicks(41);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); require(WinterFarming.greenhouse(w, CROP), "real burning native furnace provides agricultural heat");
                w.removeBlock(HEAT, false); w.setBlock(HEAT, Blocks.TORCH.defaultBlockState(), 3);
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, CROP) == 0 && !WinterFarming.greenhouse(w, CROP), "torches illuminate but never provide agricultural heat");
                w.setBlock(HEAT, Blocks.LAVA.defaultBlockState(), 3);
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, CROP) == 0, "lava is excluded from agricultural heat");
                w.setBlock(HEAT, HeatingContent.STOVE.defaultBlockState(), 3);
                ((HeatingStoveBlockEntity) w.getBlockEntity(HEAT)).setItem(0, new ItemStack(Items.COAL));
            });
            context.waitTicks(41);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); require(WinterFarming.greenhouse(w, CROP), "actual burning stove heats greenhouse");
                require(Math.abs(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, CROP) - .625) < 1e-9, "stove shared contribution is 1.25*(1-.75*4/6)");
                w.removeBlock(CROP.above(4), false);
            });
            context.waitTicks(41);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); require(!WinterFarming.greenhouse(w, CROP), "opening central roof invalidates shelter within two seconds");
                var missing = new BlockPos(1000000, 100, 1000000);
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, missing) == 0 && w.getChunkSource().getChunkNow(missing.getX() >> 4, missing.getZ() >> 4) == null, "agriculture query never loads missing chunk");
            });
        }
        ExtremeWinter.LOGGER.info("TEST D tagged native growth/phase ratios/greenhouse/bone meal/occlusion/removal/dimensions PASSED");
    }
}
