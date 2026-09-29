package dev.extremewinter.shelter;

import java.util.Set;
import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;

public final class StarterShelter {
    private static final Identifier TEMPLATE = Identifier.of(ExtremeWinter.ID, "starter_shelter");
    private static final TagKey<Block> GROUND = TagKey.of(RegistryKeys.BLOCK, Identifier.of(ExtremeWinter.ID, "shelter_ground"));
    private static final AttachmentType<Boolean> ARRIVED = AttachmentRegistry.create(
            Identifier.of(ExtremeWinter.ID, "shelter_arrival"), builder -> builder
                    .initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    private final WinterConfig config;

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
        if (template.isEmpty()) {
            state.finish("missing_template", null);
            ExtremeWinter.LOGGER.error("Starter shelter template is missing; no blocks were placed");
            return;
        }
        Vec3i size = template.get().getSize();
        // The blueprint's spawn/bed anchors assume this size; reject incompatible datapack overrides.
        if (!size.equals(new Vec3i(9, 6, 11))) {
            state.finish("invalid_template_size", null);
            ExtremeWinter.LOGGER.error("Starter shelter must be 9 x 6 x 11 blocks");
            return;
        }
        BlockPos site = findSite(world, size, false);
        if (site == null) site = findSite(world, size, true);
        if (site == null) {
            state.finish("no_safe_loaded_site", null);
            ExtremeWinter.LOGGER.warn("No safe loaded site near spawn; starter shelter skipped to preserve terrain");
            return;
        }
        // Mark before placement, so another call in this session cannot regenerate loot.
        state.finish("placing", site);
        boolean placed = template.get().place(world, site, site,
                new StructurePlacementData().setIgnoreEntities(true), world.random, Block.NOTIFY_ALL);
        state.finish(placed ? "generated" : "placement_failed", site);
        if (placed) {
            addCornerSupports(world, site, size);
            addEntranceSteps(world, site);
            world.setSpawnPos(site.add(4, 1, 2), 0);
            ExtremeWinter.LOGGER.info("Starter shelter generated at {}", site.toShortString());
        }
    }

    public void onJoin(ServerPlayerEntity player) {
        if (player.getAttachedOrCreate(ARRIVED)) return;
        player.setAttached(ARRIVED, true);
        var world = player.getServer().getOverworld();
        var state = ShelterState.get(world);
        if (!state.generated()) return;
        BlockPos pos = state.origin().add(4, 1, 7);
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), 180, 0, true);
    }

    private BlockPos findSite(ServerWorld world, Vec3i size, boolean raised) {
        BlockPos spawn = world.getSpawnPos();
        // Startup-only bounded search. Never request or generate additional chunks.
        for (int ring = 0; ring <= 5; ring++) {
            for (int dx = -ring; dx <= ring; dx++) for (int dz = -ring; dz <= ring; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                BlockPos candidate = checkSite(world, spawn.getX() + dx * 12 - 4,
                        spawn.getZ() + dz * 12 - 5, size, raised);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private BlockPos checkSite(ServerWorld world, int originX, int originZ, Vec3i size, boolean raised) {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) {
            int wx = originX + x, wz = originZ + z;
            var chunk = world.getChunkManager().getWorldChunk(wx >> 4, wz >> 4);
            if (chunk == null) return null;
            int y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, wx & 15, wz & 15) + 1;
            var surface = world.getBlockState(new BlockPos(wx, y - 1, wz));
            boolean natural = surface.isIn(GROUND);
            if (!natural && !(raised && (surface.isOf(Blocks.WATER) || surface.isIn(BlockTags.LEAVES)))) return null;
            minimum = Math.min(minimum, y);
            maximum = Math.max(maximum, y);
        }
        if (maximum - minimum > (raised ? 8 : 2)) return null;
        BlockPos origin = new BlockPos(originX, maximum, originZ);
        for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) {
            for (int y = 0; y < size.getY(); y++) {
                BlockPos pos = origin.add(x, y, z);
                if (!world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)) return null;
                var block = world.getBlockState(pos);
                if (block.hasBlockEntity()) return null;
                if (!block.isAir() && !block.isOf(Blocks.SHORT_GRASS) && !block.isOf(Blocks.TALL_GRASS)
                        && !block.isOf(Blocks.FERN) && !block.isOf(Blocks.LARGE_FERN) && !block.isOf(Blocks.SNOW)) return null;
            }
        }
        return origin;
    }

    private void addCornerSupports(ServerWorld world, BlockPos origin, Vec3i size) {
        for (int x : new int[]{0, size.getX() - 1}) for (int z : new int[]{1, size.getZ() - 1}) {
            for (int depth = 1; depth <= 8; depth++) {
                BlockPos pos = origin.add(x, -depth, z);
                if (!world.getBlockState(pos).isAir()) break;
                world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    private void addEntranceSteps(ServerWorld world, BlockPos origin) {
        // Adapt the template porch to a small terrain drop so players can return home.
        var stairs = world.getBlockState(origin.add(4, 0, 0));
        for (int step = 1; step <= 8; step++) {
            BlockPos center = origin.add(4, -step, -step);
            if (world.getChunkManager().getWorldChunk(center.getX() >> 4, center.getZ() >> 4) == null
                    || !world.isInBuildLimit(center) || !world.getWorldBorder().contains(center)) break;
            var existing = world.getBlockState(center);
            if (!existing.isAir() && !existing.isOf(Blocks.SNOW)
                    && !existing.isOf(Blocks.SHORT_GRASS) && !existing.isOf(Blocks.TALL_GRASS)) break;
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos pos = center.add(dx, 0, 0);
                if (!world.getWorldBorder().contains(pos)
                        || world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
                var block = world.getBlockState(pos);
                if (block.isAir() || block.isOf(Blocks.SNOW) || block.isOf(Blocks.SHORT_GRASS)
                        || block.isOf(Blocks.TALL_GRASS)) world.setBlockState(pos, stairs, Block.NOTIFY_ALL);
            }
        }
    }
}
