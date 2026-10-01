package dev.extremewinter.test;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.HeatWeathering;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;

/** Short fixture durations test the same persisted clocks used by normal gameplay. */
public final class HeatWeatheringTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        boolean oldfurnaceWeathering=ExtremeWinter.CONFIG.furnaceWeathering; ExtremeWinter.CONFIG.furnaceWeathering=true;
        try {
        var config = new WinterConfig();
        config.furnaceWeathering = true;
        config.lavaCooling = true;
        config.campfireExposureSeconds = 3;
        config.soulCampfireExposureSeconds = 5;
        config.furnaceExposureSeconds = 3;
        config.blastFurnaceExposureSeconds = 4;
        config.smokerExposureSeconds = 2;
        config.lavaExposureSeconds = 6;
        var weather = new HeatWeathering(config);
        var camp = new BlockPos(0, 110, 0);
        var soul = new BlockPos(3, 110, 0);
        var covered = new BlockPos(6, 110, 0);
        var furnace = new BlockPos(9, 110, 0);
        var blast = new BlockPos(12, 110, 0);
        var smoker = new BlockPos(15, 110, 0);
        var lava = new BlockPos(0, 110, 5);
        var roofedLava = new BlockPos(4, 110, 5);
        TestWorldSave save;
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                for (BlockPos pos : new BlockPos[]{camp, soul, covered, furnace, blast, smoker, lava, roofedLava}) {
                    world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
                world.setBlock(camp, Blocks.CAMPFIRE.defaultBlockState(), 3);
                world.setBlock(soul, Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
                world.setBlock(covered, Blocks.CAMPFIRE.defaultBlockState(), 3);
                ExposureTest.roof(world, covered.above(3), Blocks.GLASS.defaultBlockState());
                for (var pair : new Object[][]{{furnace, Blocks.FURNACE}, {blast, Blocks.BLAST_FURNACE}, {smoker, Blocks.SMOKER}}) {
                    BlockPos pos = (BlockPos) pair[0];
                    var block = (Block) pair[1];
                    world.setBlock(pos, block.defaultBlockState().setValue(BlockStateProperties.LIT, true), 3);
                    var entity = (AbstractFurnaceBlockEntity) world.getBlockEntity(pos);
                    entity.setItem(0, new ItemStack(block == Blocks.SMOKER ? Items.BEEF : Items.RAW_IRON, 16));
                    entity.setItem(1, new ItemStack(Items.COAL, 8));
                }
                // Enclose each source, so fluid expansion cannot alter the clock fixture.
                for (BlockPos pos : new BlockPos[]{lava, roofedLava}) {
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        world.setBlock(pos.relative(direction), Blocks.STONE.defaultBlockState(), 3);
                    }
                    world.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                }
                weather.discoverLava(world, lava);
                weather.discoverLava(world, roofedLava);
                ExposureTest.roof(world, roofedLava.above(3), Blocks.GLASS.defaultBlockState());
                for (int second = 0; second < 2; second++) {
                    for (BlockPos pos : new BlockPos[]{camp, soul, covered, furnace, blast, smoker}) {
                        weather.advanceBlockEntity(world, world.getBlockEntity(pos));
                    }
                    weather.advanceLava(world);
                }
                require(world.getBlockState(camp).getValue(BlockStateProperties.LIT), "campfire remains lit before its limit");
                require(!world.getBlockState(smoker).getValue(BlockStateProperties.LIT), "smoker has the shortest furnace duration");
                require(world.getBlockState(covered).getValue(BlockStateProperties.LIT), "glass-roofed campfire is protected");
                // Pause ordinary live ticks while saving/loading the short-duration fixture.
                for (var pos : new BlockPos[]{camp, soul, furnace, blast, lava}) {
                    ExposureTest.roof(world, pos.above(3), Blocks.GLASS.defaultBlockState());
                }
            });
            // Save/reopen inside the running regression, without resetting partially elapsed timers.
        }
        try (var reopened = save.open()) {
                reopened.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    for (var pos : new BlockPos[]{camp, soul, furnace, blast, lava}) {
                        ExposureTest.roof(world, pos.above(3), Blocks.AIR.defaultBlockState());
                    }
                    weather.advanceBlockEntity(world, world.getBlockEntity(camp));
                    weather.advanceBlockEntity(world, world.getBlockEntity(furnace));
                    require(!world.getBlockState(camp).getValue(BlockStateProperties.LIT), "campfire timer survives save/reopen");
                    require(!world.getBlockState(furnace).getValue(BlockStateProperties.LIT), "furnace actually extinguishes");
                    require(!HeatSources.isHeatSource(world.getBlockState(furnace)), "cold furnace no longer provides warmth");
                    var entity = (AbstractFurnaceBlockEntity) world.getBlockEntity(furnace);
                    require(entity.getItem(0).getCount() > 0 && entity.getItem(1).getCount() > 0, "extinguishing retains input and fuel");
                    for (int i = 0; i < 6; i++) {
                        AbstractFurnaceBlockEntity.serverTick(world, furnace, world.getBlockState(furnace), entity);
                    }
                    require(!world.getBlockState(furnace).getValue(BlockStateProperties.LIT), "exposed furnace cannot auto-reignite");
                    ExposureTest.roof(world, furnace.above(3), Blocks.GLASS.defaultBlockState());
                    AbstractFurnaceBlockEntity.serverTick(world, furnace, world.getBlockState(furnace), entity);
                    require(world.getBlockState(furnace).getValue(BlockStateProperties.LIT), "roof allows furnace to resume with retained fuel");
                    for (int i = 0; i < 2; i++) weather.advanceBlockEntity(world, world.getBlockEntity(blast));
                    require(!world.getBlockState(blast).getValue(BlockStateProperties.LIT), "blast furnace uses its own duration");
                    for (int i = 0; i < 2; i++) weather.advanceBlockEntity(world, world.getBlockEntity(soul));
                    require(world.getBlockState(soul).getValue(BlockStateProperties.LIT), "soul campfire lasts longer than regular campfire");
                    weather.advanceBlockEntity(world, world.getBlockEntity(soul));
                    require(!world.getBlockState(soul).getValue(BlockStateProperties.LIT), "soul campfire expires at its own limit");
                    for (int i = 0; i < 4; i++) weather.advanceLava(world);
                    require(world.getBlockState(lava).is(Blocks.OBSIDIAN), "source lava clock survives reload and becomes obsidian");
                    require(world.getBlockState(roofedLava).is(Blocks.LAVA), "covered lava does not cool");
                    world.setBlock(camp, Blocks.CAMPFIRE.defaultBlockState(), 3);
                    weather.advanceBlockEntity(world, world.getBlockEntity(camp));
                    require(world.getBlockState(camp).getValue(BlockStateProperties.LIT), "manually relit campfire gets a new lifetime");
                    require(ExtremeWinter.CONFIG.lavaExposureSeconds == 3600, "normal lava default is three game days");
                });
        }
        } finally {
            ExtremeWinter.CONFIG.furnaceWeathering=oldfurnaceWeathering;
        }
        ExtremeWinter.LOGGER.info("TEST heat weathering, furnace fuel preservation and persistent lava cooling PASSED");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
