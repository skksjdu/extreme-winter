package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.Exposure;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;

public final class HeatWeathering {
    public static final AttachmentType<Integer> EXPOSURE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "heat_exposure_seconds"), b -> b.initializer(() -> 0).persistent(Codec.INT));
    public static final AttachmentType<Boolean> BLOCKED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "furnace_weather_blocked"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    public static final AttachmentType<Boolean> WAS_LIT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "heat_was_lit"), b -> b.initializer(() -> true).persistent(Codec.BOOL));
    private final WinterConfig config;
    private int lavaChunkCursor;
    private final TorchWeathering torches;

    public HeatWeathering(WinterConfig config) { this.config = config; this.torches = new TorchWeathering(config); }

    public void onChunkLoad(ServerLevel world, LevelChunk chunk) { torches.discoverChunk(world, chunk); }

    public void tick(ServerLevel world) {
        if (!config.outdoorHeatExtinguishing || !world.dimension().equals(Level.OVERWORLD)
                || world.getGameTime() % 20 != 0) return;
        var chunks = new LinkedHashSet<ChunkPos>();
        for (var player : world.players()) {
            if (player.isSpectator()) continue;
            var center = player.chunkPosition();
            for (int dx = -config.simulationRadiusChunks; dx <= config.simulationRadiusChunks; dx++) {
                for (int dz = -config.simulationRadiusChunks; dz <= config.simulationRadiusChunks; dz++) {
                    var chunk = world.getChunkSource().getChunkNow(center.x() + dx, center.z() + dz);
                    if (chunk != null) chunks.add(chunk.getPos());
                }
            }
        }
        for (var pos : chunks) {
            var chunk = world.getChunkSource().getChunkNow(pos.x(), pos.z());
            for (var entity : new ArrayList<>(chunk.getBlockEntities().values())) advanceBlockEntity(world, entity);
        }
        // One already-loaded surface chunk per second; an 81-chunk radius is discovered within 81 seconds.
        if (!chunks.isEmpty()) {
            var positions = new ArrayList<>(chunks);
            var pos = positions.get(Math.floorMod(lavaChunkCursor++, positions.size()));
            var chunk = world.getChunkSource().getChunkNow(pos.x(), pos.z());
            torches.discoverChunk(world, chunk);
            if (config.lavaCooling) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                discoverLava(world, new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
            }
        }
        if (config.lavaCooling) advanceLava(world);
        torches.advance(world, chunks);
    }

    public int exposureLimit(BlockState state) {
        return HeatItems.limit(state.getBlock().asItem(), config);
    }

    public void advanceBlockEntity(ServerLevel world, BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        var state = world.getBlockState(pos);
        int limit = exposureLimit(state);
        if (limit == 0) return;
        if (!state.getValue(BlockStateProperties.LIT)) {
            entity.setAttached(WAS_LIT, false);
            return;
        }
        if (!entity.getAttachedOrCreate(WAS_LIT) && !(entity instanceof AbstractFurnaceBlockEntity)) {
            setExposure(entity, 0); // An explicit manual relight starts a new campfire period.
        }
        entity.setAttached(WAS_LIT, true);
        if (!Exposure.outdoors(world, pos.above())) return;
        int seconds = Math.min(limit, HeatItems.elapsed(entity) + 1);
        setExposure(entity, seconds);
        if (seconds < limit) return;
        if (entity instanceof AbstractFurnaceBlockEntity furnace) {
            // Read back the complete existing NBT, changing only active burn time. Inventory/components survive.
            var nbt = furnace.saveWithoutMetadata(world.registryAccess());
            nbt.putShort("lit_time_remaining", (short) 0);
            nbt.putShort("lit_total_time", (short) 0);
            furnace.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), nbt));
            furnace.setAttached(BLOCKED, true);
        } else {
            CampfireBlock.dowse(null, world, pos, state);
        }
        world.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        entity.setAttached(WAS_LIT, false);
        entity.setChanged();
    }

    public static boolean furnaceBlocked(ServerLevel world, BlockPos pos, AbstractFurnaceBlockEntity furnace) {
        if (!furnace.getAttachedOrCreate(BLOCKED)) return false;
        if (ExtremeWinter.CONFIG.outdoorHeatExtinguishing && ExtremeWinter.CONFIG.furnaceWeathering && world.dimension().equals(Level.OVERWORLD)
                && Exposure.outdoors(world, pos.above())) return true;
        furnace.setAttached(BLOCKED, false);
        setExposure(furnace, 0);
        return false;
    }

    private static void setExposure(BlockEntity entity, int seconds) {
        if (HeatItems.elapsed(entity) == seconds) return;
        HeatItems.writeExposure(entity, seconds);
    }

    public void discoverLava(ServerLevel world, BlockPos pos) {
        if (!config.outdoorHeatExtinguishing || !config.lavaCooling || !world.hasChunkAt(pos)) return;
        var state = world.getBlockState(pos);
        if (!state.is(Blocks.LAVA) || !state.getFluidState().isSource()
                || !Exposure.outdoors(world, pos.above())) return;
        var cooling = LavaCoolingState.get(world);
        if (cooling.seconds.putIfAbsent(Long.toString(pos.asLong()), 0) == null) cooling.setDirty();
    }

    public void advanceLava(ServerLevel world) {
        if (!config.outdoorHeatExtinguishing || !config.lavaCooling) return;
        var cooling = LavaCoolingState.get(world);
        var entries = cooling.seconds.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            BlockPos pos = BlockPos.of(Long.parseLong(entry.getKey()));
            if (world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            var state = world.getBlockState(pos);
            if (!state.is(Blocks.LAVA) || !state.getFluidState().isSource()) {
                entries.remove();
                cooling.setDirty();
                continue;
            }
            if (!Exposure.outdoors(world, pos.above())) continue;
            int seconds = Math.min(config.lavaExposureSeconds, entry.getValue() + 1);
            if (seconds >= config.lavaExposureSeconds) {
                world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                entries.remove();
            } else entry.setValue(seconds);
            cooling.setDirty();
        }
    }
}
