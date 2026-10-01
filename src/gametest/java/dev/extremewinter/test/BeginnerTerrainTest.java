package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Scripted survival operations on five real generated terrains, with ordinary starter supplies. */
public final class BeginnerTerrainTest implements FabricClientGameTest {
    private static void require(boolean b, String message) { if (!b) throw new AssertionError(message); }
    public void runTest(ClientGameTestContext context) {
        var biomes = java.util.List.of(Biomes.PLAINS, Biomes.SNOWY_PLAINS, Biomes.DESERT, Biomes.FOREST, Biomes.MUSHROOM_FIELDS);
        String[] kinds = {"plains", "snowfield", "desert", "forest", "sea-island"};
        for (int index = 0; index < biomes.size(); index++) {
            ResourceKey<Biome> wanted = biomes.get(index); String kind = kinds[index]; String seed = "20261001" + index;
            BlockPos[] shelter = new BlockPos[1], surface = new BlockPos[1];
            try (var game = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(creator -> {
                creator.setSeed(seed); creator.getGameRules().set(GameRules.SPAWN_MOBS, false, null);
            }).create()) {
                game.getServer().runOnServer(server -> {
                    var world = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                    require(!p.isCreative() && p.getInventory().countItem(Items.CAMPFIRE) == 1, "ordinary survival spawn with starter campfire");
                    var found = world.findClosestBiome3d(h -> h.is(wanted), world.getRespawnData().pos(), 16000, 64, 64);
                    require(found != null, "fixed natural seed contains " + kind);
                    var target = found.getFirst();
                    // Only the harness loads the selected test site; production queries never load chunks.
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) world.getChunk((target.getX() >> 4) + dx, (target.getZ() >> 4) + dz);
                    int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
                    surface[0] = new BlockPos(target.getX(), ground, target.getZ());
                    p.teleportTo(world, target.getX() + .5, ground, target.getZ() + .5, Set.of(), 0, 0, true);
                });
                context.waitTicks(40);
                game.getServer().runOnServer(server -> {
                    var world = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                    require(world.getChunkSource().getChunkNow(surface[0].getX() >> 4, surface[0].getZ() >> 4) != null, "terrain is fully loaded before shelter checks");
                    p.getInventory().setSelectedSlot(p.getInventory().findSlotMatchingItem(new ItemStack(Items.STONE_SHOVEL)));
                    for (int x = -1; x <= 1; x++) require(p.gameMode.destroyBlock(surface[0].below().offset(x, 0, 0)), "actual surface gathering with starter shovel");
                });
                context.waitTicks(20);
                game.getServer().runOnServer(server -> {
                    var world = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                    BlockPos base = null;
                    for (int depth = 7; depth <= 32 && base == null; depth++) {
                        var candidate = surface[0].offset(2, -depth, 2);
                        int covered = 0;
                        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                            var top = candidate.offset(x, 2, z);
                            if (world.getChunkSource().getChunkNow(top.getX() >> 4, top.getZ() >> 4) != null
                                    && world.getBlockState(top).isSolid()) covered++;
                        }
                        if (covered == 9 && world.getBlockState(candidate.below()).isSolid()) base = candidate;
                    }
                    require(base != null, "find a naturally solid roof and floor near the gathering site");
                    shelter[0] = base;
                    require(world.getBiome(surface[0]).is(wanted), "actual generated site matches " + kind);
                    p.teleportTo(world, base.getX() + .5, base.getY(), base.getZ() + .5, Set.of(), 0, 0, true);
                    int broken = 0;
                    for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y <= 1; y++) {
                        var pos = base.offset(x, y, z);
                        if (!world.getBlockState(pos).isAir()) require(p.gameMode.destroyBlock(pos), "vanilla survival breaking carves first cave shelter");
                        broken++;
                    }
                    require(!Exposure.outdoors(world, base.above()), "natural cave roof provides shelter without rare materials: " + kind + " " + base);
                    var campPos = base.offset(1, 0, 0); var support = campPos.below();
                    var camp = p.getInventory().removeItem(p.getInventory().findSlotMatchingItem(new ItemStack(Items.CAMPFIRE)), 1);
                    require(camp.is(Items.CAMPFIRE), "use actual starter campfire");
                    p.setItemInHand(InteractionHand.MAIN_HAND, camp);
                    require(((BlockItem) camp.getItem()).place(new BlockPlaceContext(p, InteractionHand.MAIN_HAND, camp,
                            new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false))).consumesAction(), "campfire really places on natural floor");
                    require(new HeatSources(ExtremeWinter.CONFIG).strength(p) > 0, "sheltered natural site receives usable campfire heat");
                    TemperatureData.set(p, 80);
                    ExtremeWinter.LOGGER.info("TEST C terrain seed={} kind={} biome={} shelter={} broken={}", seed, kind, wanted.identifier(), base, broken);
                });
                context.waitTicks(40);
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    require(TemperatureData.get(p) > 80, "actual two seconds in shelter returns warmth on " + kind);
                    int gathered = p.getInventory().countItem(Items.DIRT) + p.getInventory().countItem(Items.SAND) + p.getInventory().countItem(Items.SNOWBALL);
                    ExtremeWinter.LOGGER.info("TEST C terrain {} gathered basic drops={} warmth={}", kind, gathered, TemperatureData.get(p));
                });
                game.getClientLevel().waitForChunksRender(); context.takeScreenshot("beginner-C-terrain-" + kind);
            }
        }
        ExtremeWinter.LOGGER.info("TEST C five natural seeds, survival excavation, starter placement and shelter warming PASSED");
    }
}
