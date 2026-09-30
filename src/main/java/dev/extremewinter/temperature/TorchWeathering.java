package dev.extremewinter.temperature;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.Exposure;

public final class TorchWeathering {
    private final WinterConfig config;
    public TorchWeathering(WinterConfig config) { this.config = config; }

    public void discoverChunk(ServerLevel world, LevelChunk chunk) {
        if (!config.outdoorHeatExtinguishing || !world.dimension().equals(Level.OVERWORLD)) return;
        var cooling = TorchCoolingState.get(world);
        var sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (!section.maybeHas(HeatItems::isTorch)) continue;
            int baseY = chunk.getMinY() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 16; y++) {
                if (!HeatItems.isTorch(section.getBlockState(x, y, z))) continue;
                var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                if (!cooling.contains(pos)) cooling.track(pos, 0);
            }
        }
    }

    public void advance(ServerLevel world, Set<ChunkPos> loadedChunks) {
        var cooling = TorchCoolingState.get(world);
        for (var chunk : loadedChunks) for (var key : cooling.inChunk(chunk)) {
            var pos = BlockPos.of(Long.parseLong(key));
            var state = world.getBlockState(pos);
            if (!HeatItems.isTorch(state)) { cooling.remove(pos); continue; }
            if (!Exposure.outdoors(world, pos.above())) continue;
            int elapsed = Math.min(config.torchExposureSeconds, cooling.elapsed(pos) + 1);
            cooling.track(pos, elapsed);
            if (elapsed < config.torchExposureSeconds) continue;
            var stack = new ItemStack(state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                    ? Items.SOUL_TORCH : Items.TORCH);
            stack.set(HeatItems.EXPOSURE, config.torchExposureSeconds);
            if (!world.removeBlock(pos, false)) continue;
            cooling.remove(pos);
            var item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, stack);
            item.setDefaultPickUpDelay();
            world.addFreshEntity(item);
        }
    }
}
