package dev.extremewinter.temperature;

import java.util.Set;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.WinterEnvironment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.block.Blocks;

public final class TorchWeathering {
    private final WinterConfig config;
    public TorchWeathering(WinterConfig config) { this.config = config; }

    public void discoverChunk(ServerWorld world, WorldChunk chunk) {
        if (!config.outdoorHeatExtinguishing || !world.getRegistryKey().equals(World.OVERWORLD)) return;
        var cooling = TorchCoolingState.get(world);
        var sections = chunk.getSectionArray();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (!section.hasAny(HeatItems::isTorch)) continue;
            int baseY = chunk.getBottomY() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 16; y++) {
                if (!HeatItems.isTorch(section.getBlockState(x, y, z))) continue;
                var pos = new BlockPos(chunk.getPos().getStartX() + x, baseY + y, chunk.getPos().getStartZ() + z);
                if (!cooling.contains(pos)) cooling.track(pos, 0);
            }
        }
    }

    public void advance(ServerWorld world, Set<ChunkPos> loadedChunks) {
        var cooling = TorchCoolingState.get(world);
        for (var chunk : loadedChunks) for (var key : cooling.inChunk(chunk)) {
            var pos = BlockPos.fromLong(Long.parseLong(key));
            var state = world.getBlockState(pos);
            if (!HeatItems.isTorch(state)) { cooling.remove(pos); continue; }
            if (!WinterEnvironment.isExposedSurface(world, pos)) continue;
            int elapsed = Math.min(config.torchExposureSeconds, cooling.elapsed(pos) + 1);
            cooling.track(pos, elapsed);
            if (elapsed < config.torchExposureSeconds) continue;
            var stack = new ItemStack(state.isOf(Blocks.SOUL_TORCH) || state.isOf(Blocks.SOUL_WALL_TORCH)
                    ? Items.SOUL_TORCH : Items.TORCH);
            stack.set(HeatItems.EXPOSURE, config.torchExposureSeconds);
            if (!world.removeBlock(pos, false)) continue;
            cooling.remove(pos);
            var item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, stack);
            item.setToDefaultPickupDelay();
            world.spawnEntity(item);
        }
    }
}
