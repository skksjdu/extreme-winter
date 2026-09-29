package dev.extremewinter.test;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.HeatWeathering;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

/** Short fixture durations test the same persisted clocks used by normal gameplay. */
public final class HeatWeatheringTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var config = new WinterConfig();
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
                var world = server.getOverworld();
                for (BlockPos pos : new BlockPos[]{camp, soul, covered, furnace, blast, smoker, lava, roofedLava}) {
                    world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
                }
                world.setBlockState(camp, Blocks.CAMPFIRE.getDefaultState());
                world.setBlockState(soul, Blocks.SOUL_CAMPFIRE.getDefaultState());
                world.setBlockState(covered, Blocks.CAMPFIRE.getDefaultState());
                world.setBlockState(covered.up(3), Blocks.GLASS.getDefaultState());
                for (var pair : new Object[][]{{furnace, Blocks.FURNACE}, {blast, Blocks.BLAST_FURNACE}, {smoker, Blocks.SMOKER}}) {
                    BlockPos pos = (BlockPos) pair[0];
                    var block = (Block) pair[1];
                    world.setBlockState(pos, block.getDefaultState().with(Properties.LIT, true));
                    var entity = (AbstractFurnaceBlockEntity) world.getBlockEntity(pos);
                    entity.setStack(0, new ItemStack(block == Blocks.SMOKER ? Items.BEEF : Items.RAW_IRON, 16));
                    entity.setStack(1, new ItemStack(Items.COAL, 8));
                }
                // Enclose each source, so fluid expansion cannot alter the clock fixture.
                for (BlockPos pos : new BlockPos[]{lava, roofedLava}) {
                    for (var direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                        world.setBlockState(pos.offset(direction), Blocks.STONE.getDefaultState());
                    }
                    world.setBlockState(pos, Blocks.LAVA.getDefaultState());
                }
                weather.discoverLava(world, lava);
                weather.discoverLava(world, roofedLava);
                world.setBlockState(roofedLava.up(3), Blocks.GLASS.getDefaultState());
                for (int second = 0; second < 2; second++) {
                    for (BlockPos pos : new BlockPos[]{camp, soul, covered, furnace, blast, smoker}) {
                        weather.advanceBlockEntity(world, world.getBlockEntity(pos));
                    }
                    weather.advanceLava(world);
                }
                require(world.getBlockState(camp).get(Properties.LIT), "campfire remains lit before its limit");
                require(!world.getBlockState(smoker).get(Properties.LIT), "smoker has the shortest furnace duration");
                require(world.getBlockState(covered).get(Properties.LIT), "glass-roofed campfire is protected");
            });
            // Save/reopen inside the running regression, without resetting partially elapsed timers.
        }
        try (var reopened = save.open()) {
                reopened.getServer().runOnServer(server -> {
                    var world = server.getOverworld();
                    weather.advanceBlockEntity(world, world.getBlockEntity(camp));
                    weather.advanceBlockEntity(world, world.getBlockEntity(furnace));
                    require(!world.getBlockState(camp).get(Properties.LIT), "campfire timer survives save/reopen");
                    require(!world.getBlockState(furnace).get(Properties.LIT), "furnace actually extinguishes");
                    require(!HeatSources.isHeatSource(world.getBlockState(furnace)), "cold furnace no longer provides warmth");
                    var entity = (AbstractFurnaceBlockEntity) world.getBlockEntity(furnace);
                    require(entity.getStack(0).getCount() > 0 && entity.getStack(1).getCount() > 0, "extinguishing retains input and fuel");
                    for (int i = 0; i < 6; i++) {
                        AbstractFurnaceBlockEntity.tick(world, furnace, world.getBlockState(furnace), entity);
                    }
                    require(!world.getBlockState(furnace).get(Properties.LIT), "exposed furnace cannot auto-reignite");
                    world.setBlockState(furnace.up(3), Blocks.GLASS.getDefaultState());
                    AbstractFurnaceBlockEntity.tick(world, furnace, world.getBlockState(furnace), entity);
                    require(world.getBlockState(furnace).get(Properties.LIT), "roof allows furnace to resume with retained fuel");
                    for (int i = 0; i < 2; i++) weather.advanceBlockEntity(world, world.getBlockEntity(blast));
                    require(!world.getBlockState(blast).get(Properties.LIT), "blast furnace uses its own duration");
                    for (int i = 0; i < 2; i++) weather.advanceBlockEntity(world, world.getBlockEntity(soul));
                    require(world.getBlockState(soul).get(Properties.LIT), "soul campfire lasts longer than regular campfire");
                    weather.advanceBlockEntity(world, world.getBlockEntity(soul));
                    require(!world.getBlockState(soul).get(Properties.LIT), "soul campfire expires at its own limit");
                    for (int i = 0; i < 4; i++) weather.advanceLava(world);
                    require(world.getBlockState(lava).isOf(Blocks.OBSIDIAN), "source lava clock survives reload and becomes obsidian");
                    require(world.getBlockState(roofedLava).isOf(Blocks.LAVA), "covered lava does not cool");
                    world.setBlockState(camp, Blocks.CAMPFIRE.getDefaultState());
                    weather.advanceBlockEntity(world, world.getBlockEntity(camp));
                    require(world.getBlockState(camp).get(Properties.LIT), "manually relit campfire gets a new lifetime");
                    require(ExtremeWinter.CONFIG.lavaExposureSeconds == 3600, "normal lava default is three game days");
                });
        }
        ExtremeWinter.LOGGER.info("TEST heat weathering, furnace fuel preservation and persistent lava cooling PASSED");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
