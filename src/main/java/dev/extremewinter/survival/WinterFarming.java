package dev.extremewinter.survival;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.WinterProgression;
import dev.extremewinter.temperature.WinterWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class WinterFarming {
    public static final TagKey<Block> CROPS = TagKey.create(Registries.BLOCK, WinterGear.id("winter_crops"));
    private static final HeatSources HEAT = new HeatSources(ExtremeWinter.CONFIG);
    private WinterFarming() { }
    public static boolean greenhouse(ServerLevel world, BlockPos pos) {
        return world.dimension().equals(Level.OVERWORLD) && world.getRawBrightness(pos, 0) >= 9
                && HEAT.shelteredAt(world, pos) && HEAT.strengthAt(world, pos) >= .25;
    }
    public static boolean allowGrowth(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!state.is(CROPS) || !world.dimension().equals(Level.OVERWORLD)) return true;
        int stage = WinterProgression.stage(WinterWorldState.get(world).elapsedTicks(), ExtremeWinter.CONFIG);
        if (stage < 3) return true;
        double chance = WinterFarmingModel.growthChance(stage, true, greenhouse(world, pos));
        return chance == 1 || random.nextDouble() < chance;
    }
}
