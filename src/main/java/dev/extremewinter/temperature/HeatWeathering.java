package dev.extremewinter.temperature;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.WinterEnvironment;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public final class HeatWeathering {
    public static final AttachmentType<Integer> EXPOSURE = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "heat_exposure_seconds"), b -> b.initializer(() -> 0).persistent(Codec.INT));
    public static final AttachmentType<Boolean> BLOCKED = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "furnace_weather_blocked"), b -> b.initializer(() -> false).persistent(Codec.BOOL));
    public static final AttachmentType<Boolean> WAS_LIT = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "heat_was_lit"), b -> b.initializer(() -> true).persistent(Codec.BOOL));
    private final WinterConfig config;
    private int lavaChunkCursor;

    public HeatWeathering(WinterConfig config) { this.config = config; }

    public void tick(ServerWorld world) {
        if (!config.outdoorHeatExtinguishing || !world.getRegistryKey().equals(World.OVERWORLD)
                || world.getTime() % 20 != 0) return;
        var chunks = new LinkedHashSet<ChunkPos>();
        for (var player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            var center = player.getChunkPos();
            for (int dx = -config.simulationRadiusChunks; dx <= config.simulationRadiusChunks; dx++) {
                for (int dz = -config.simulationRadiusChunks; dz <= config.simulationRadiusChunks; dz++) {
                    var chunk = world.getChunkManager().getWorldChunk(center.x + dx, center.z + dz);
                    if (chunk != null) chunks.add(chunk.getPos());
                }
            }
        }
        for (var pos : chunks) {
            var chunk = world.getChunkManager().getWorldChunk(pos.x, pos.z);
            for (var entity : new ArrayList<>(chunk.getBlockEntities().values())) advanceBlockEntity(world, entity);
        }
        // One already-loaded surface chunk per second; an 81-chunk radius is discovered within 81 seconds.
        if (!chunks.isEmpty()) {
            var positions = new ArrayList<>(chunks);
            var pos = positions.get(Math.floorMod(lavaChunkCursor++, positions.size()));
            var chunk = world.getChunkManager().getWorldChunk(pos.x, pos.z);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x, z);
                discoverLava(world, new BlockPos(pos.getStartX() + x, y, pos.getStartZ() + z));
            }
        }
        advanceLava(world);
    }

    public int exposureLimit(BlockState state) {
        return HeatItems.limit(state.getBlock().asItem(), config);
    }

    public void advanceBlockEntity(ServerWorld world, BlockEntity entity) {
        BlockPos pos = entity.getPos();
        var state = world.getBlockState(pos);
        int limit = exposureLimit(state);
        if (limit == 0) return;
        if (!state.get(Properties.LIT)) {
            entity.setAttached(WAS_LIT, false);
            return;
        }
        if (!entity.getAttachedOrCreate(WAS_LIT) && !(entity instanceof AbstractFurnaceBlockEntity)) {
            setExposure(entity, 0); // An explicit manual relight starts a new campfire period.
        }
        entity.setAttached(WAS_LIT, true);
        if (!WinterEnvironment.isExposedSurface(world, pos)) return;
        int seconds = Math.min(limit, HeatItems.elapsed(entity) + 1);
        setExposure(entity, seconds);
        if (seconds < limit) return;
        if (entity instanceof AbstractFurnaceBlockEntity furnace) {
            // Read back the complete existing NBT, changing only active burn time. Inventory/components survive.
            var nbt = furnace.createNbt(world.getRegistryManager());
            nbt.putShort("lit_time_remaining", (short) 0);
            nbt.putShort("lit_total_time", (short) 0);
            furnace.read(NbtReadView.create(ErrorReporter.EMPTY, world.getRegistryManager(), nbt));
            furnace.setAttached(BLOCKED, true);
        } else {
            CampfireBlock.extinguish(null, world, pos, state);
        }
        world.setBlockState(pos, state.with(Properties.LIT, false), Block.NOTIFY_ALL);
        entity.setAttached(WAS_LIT, false);
        entity.markDirty();
    }

    public static boolean furnaceBlocked(ServerWorld world, BlockPos pos, AbstractFurnaceBlockEntity furnace) {
        if (!furnace.getAttachedOrCreate(BLOCKED)) return false;
        if (ExtremeWinter.CONFIG.outdoorHeatExtinguishing && world.getRegistryKey().equals(World.OVERWORLD)
                && WinterEnvironment.isExposedSurface(world, pos)) return true;
        furnace.setAttached(BLOCKED, false);
        setExposure(furnace, 0);
        return false;
    }

    private static void setExposure(BlockEntity entity, int seconds) {
        if (HeatItems.elapsed(entity) == seconds) return;
        HeatItems.writeExposure(entity, seconds);
    }

    public void discoverLava(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) return;
        var state = world.getBlockState(pos);
        if (!state.isOf(Blocks.LAVA) || !state.getFluidState().isStill()
                || !WinterEnvironment.isExposedSurface(world, pos)) return;
        var cooling = LavaCoolingState.get(world);
        if (cooling.seconds.putIfAbsent(Long.toString(pos.asLong()), 0) == null) cooling.markDirty();
    }

    public void advanceLava(ServerWorld world) {
        var cooling = LavaCoolingState.get(world);
        var entries = cooling.seconds.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            BlockPos pos = BlockPos.fromLong(Long.parseLong(entry.getKey()));
            if (world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            var state = world.getBlockState(pos);
            if (!state.isOf(Blocks.LAVA) || !state.getFluidState().isStill()) {
                entries.remove();
                cooling.markDirty();
                continue;
            }
            if (!WinterEnvironment.isExposedSurface(world, pos)) continue;
            int seconds = Math.min(config.lavaExposureSeconds, entry.getValue() + 1);
            if (seconds >= config.lavaExposureSeconds) {
                world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
                entries.remove();
            } else entry.setValue(seconds);
            cooling.markDirty();
        }
    }
}
