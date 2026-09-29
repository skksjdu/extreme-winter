package dev.extremewinter.shelter;

import java.util.Set;
import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;

public final class StarterShelter {
    private static final Identifier TEMPLATE = Identifier.of(ExtremeWinter.ID, "starter_shelter");
    private static final TagKey<Block> GROUND = TagKey.of(RegistryKeys.BLOCK, Identifier.of(ExtremeWinter.ID, "shelter_ground"));
    private static final AttachmentType<Boolean> ARRIVED = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "shelter_arrival"), b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    private final WinterConfig config;
    private record Site(BlockPos origin, BlockRotation rotation, boolean mound, int entranceRise) {
        BlockPos at(int x, int y, int z) { return origin.add(new BlockPos(x, y, z).rotate(rotation)); }
    }

    public StarterShelter(WinterConfig config) { this.config = config; }

    public void onStarted(MinecraftServer server) {
        var world = server.getOverworld();
        var state = ShelterState.get(world);
        if (!state.outcome().equals("pending")) return;
        if (!config.starterShelter || world.getTime() != 0) {
            state.finish(config.starterShelter ? "skipped_existing_world" : "disabled", null);
            return;
        }
        var template = world.getStructureTemplateManager().getTemplate(TEMPLATE);
        if (template.isEmpty() || !template.get().getSize().equals(new Vec3i(9, 6, 11))) {
            state.finish("invalid_template", null);
            ExtremeWinter.LOGGER.error("Mountain shelter template must be 9 x 6 x 11 blocks");
            return;
        }
        Site site = findSite(world, false);
        if (site == null) site = findSite(world, true);
        if (site == null) {
            state.finish("no_safe_loaded_site", null);
            ExtremeWinter.LOGGER.warn("No safe loaded mountain shelter site; skipped without touching terrain");
            return;
        }
        state.setRotation(site.rotation());
        state.finish("placing", site.origin());
        if (site.mound()) buildMound(world, site);
        boolean placed = template.get().place(world, site.origin(), site.origin(),
                new StructurePlacementData().setIgnoreEntities(true).setRotation(site.rotation()), world.random, Block.NOTIFY_ALL);
        if (placed) carveEntrance(world, site);
        state.finish(placed ? "generated" : "placement_failed", site.origin());
        if (placed) {
            world.setSpawnPos(site.at(4, 1, 2), state.arrivalYaw());
            ExtremeWinter.LOGGER.info("Mountain shelter generated at {} ({}, {})", site.origin().toShortString(),
                    site.rotation(), site.mound() ? "snow-covered rock mound fallback" : "natural hillside");
        }
    }

    public void onJoin(ServerPlayerEntity player) {
        if (player.getAttachedOrCreate(ARRIVED)) return;
        player.setAttached(ARRIVED, true);
        var world = player.getServer().getOverworld();
        var state = ShelterState.get(world);
        if (!state.generated()) return;
        BlockPos pos = state.at(4, 1, 7);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), state.arrivalYaw(), 0, true);
    }

    private Site findSite(ServerWorld world, boolean mound) {
        BlockPos spawn = world.getSpawnPos();
        // Startup-only search, four hillside orientations, no extra chunk generation.
        for (int ring = 0; ring <= 8; ring++) {
            for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                for (BlockRotation rotation : BlockRotation.values()) {
                    int x = spawn.getX() + dx * 12, z = spawn.getZ() + dz * 12;
                    if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) continue;
                    var entrance = new BlockPos(4, 0, -1).rotate(rotation);
                    int ex = x + entrance.getX(), ez = z + entrance.getZ();
                    if (world.getChunkManager().getWorldChunk(ex >> 4, ez >> 4) == null) continue;
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ex, ez) - 1;
                    var candidate = new Site(new BlockPos(x, y, z), rotation, mound, 0);
                    if (checkSite(world, candidate)) return candidate;
                    if (!mound) {
                        candidate = new Site(new BlockPos(x, y - 3, z), rotation, false, 3);
                        if (checkSite(world, candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean checkSite(ServerWorld world, Site site) {
        int covered = 0;
        int margin = site.mound() ? 6 : 1;
        for (int x = -margin; x <= 8 + margin; x++) for (int z = -margin; z <= 10 + margin; z++) {
            BlockPos column = site.at(x, 0, z);
            if (world.getChunkManager().getWorldChunk(column.getX() >> 4, column.getZ() >> 4) == null) return false;
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            var surface = world.getBlockState(new BlockPos(column.getX(), top - 1, column.getZ()));
            if (!surface.isIn(GROUND)) return false;
            if (site.mound() && (top > site.origin().getY() + 2 || top < site.origin().getY() - 5)) return false;
            if (x >= 0 && x <= 8 && z >= 1 && z <= 10) {
                if (top >= site.origin().getY() + 8 && world.getBlockState(site.at(x, 6, z)).isIn(GROUND)) covered++;
            }
            int maxY = site.mound() ? 13 : 8;
            for (int y = 0; y <= maxY; y++) {
                BlockPos pos = site.at(x, y, z);
                if (!world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)
                        || !replaceable(world.getBlockState(pos))) return false;
            }
        }
        if (!site.mound() && (covered < 60 || !world.getBlockState(site.at(4, 7, 7)).isIn(GROUND))) return false;
        for (int z = -1; z >= -6; z--) for (int x = 3; x <= 5; x++) for (int y = 0; y <= 3 + site.entranceRise(); y++) {
            BlockPos pos = site.at(x, y, z);
            if (!world.isChunkLoaded(pos) || !world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)
                    || !replaceable(world.getBlockState(pos))) return false;
        }
        return true;
    }

    private static boolean replaceable(BlockState state) {
        return !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && (state.isAir() || state.isIn(GROUND) || state.isOf(Blocks.SHORT_GRASS)
                || state.isOf(Blocks.TALL_GRASS) || state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN));
    }

    private void buildMound(ServerWorld world, Site site) {
        for (int x = -6; x <= 14; x++) for (int z = -6; z <= 16; z++) {
            double dx = (x - 4) / 11.0, dz = (z - 5) / 12.0;
            double distance = dx * dx + dz * dz;
            if (distance >= 1) continue;
            int height = (int) Math.round(12 * Math.sqrt(1 - distance));
            for (int y = -5; y <= height; y++) {
                BlockPos pos = site.at(x, y, z);
                if (!world.isInBuildLimit(pos) || (y < 0 && !world.getBlockState(pos).isAir())) continue;
                var block = y == height ? Blocks.SNOW_BLOCK : y > height - 3 ? Blocks.STONE :
                        ((x + z + y) % 7 == 0 ? Blocks.ANDESITE : Blocks.STONE);
                world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private void carveEntrance(ServerWorld world, Site site) {
        for (int z = -1; z >= -6; z--) for (int x = 3; x <= 5; x++) {
            int rise = Math.min(site.entranceRise(), -z - 1);
            var floor = Blocks.COBBLESTONE.getDefaultState();
            if (rise > 0 && -z - 1 <= site.entranceRise()) {
                floor = Blocks.COBBLESTONE_STAIRS.getDefaultState().with(net.minecraft.state.property.Properties.HORIZONTAL_FACING,
                        site.rotation().rotate(net.minecraft.util.math.Direction.NORTH));
            }
            world.setBlockState(site.at(x, rise, z), floor, Block.NOTIFY_ALL);
            for (int y = rise + 1; y <= rise + 3; y++) world.setBlockState(site.at(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
