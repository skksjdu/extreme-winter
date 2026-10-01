package dev.extremewinter.test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.survival.WinterItems;
import dev.extremewinter.survival.WinterSupplies;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

public final class WinterSuppliesTest implements FabricClientGameTest {
    private static void require(boolean b, String message) { if (!b) throw new AssertionError(message); }
    private static ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
    }
    private static boolean extra(ItemStack s) { return s.is(WinterItems.THERMAL_LINING) || s.is(WinterItems.HOT_WATER_BOTTLE) || s.is(WinterItems.WARMING_STEW); }
    private static final BlockPos CHEST = new BlockPos(1, 100, 1);
    public void runTest(ClientGameTestContext context) {
        boolean previous = ExtremeWinter.CONFIG.structureSupplies;
        CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        java.util.List<ItemStack> stored = new ArrayList<>();
        try {
            ExtremeWinter.CONFIG.structureSupplies = true;
            try (var game = context.worldBuilder().create()) {
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    var params = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(CHEST)).create(LootContextParamSets.CHEST);
                    for (String path : WinterSupplies.TABLES) {
                        var loot = server.reloadableRegistries().getLootTable(table(path));
                        int vanilla = 0, added = 0;
                        for (int i = 1; i <= 180; i++) for (var stack : loot.getRandomItems(params, i)) {
                            if (extra(stack)) added++; else vanilla++;
                            if (stack.is(WinterItems.HOT_WATER_BOTTLE)) require(!stack.getOrDefault(dev.extremewinter.survival.WinterGear.FILLED, false), "loot bottles are empty");
                        }
                        require(vanilla > 0 && added > 0, "registered table appends supplies and retains original loot: " + path);
                    }
                    var plains = server.reloadableRegistries().getLootTable(table("chests/village/village_plains_house"));
                    int[] counts = new int[4]; int events = 0;
                    for (int i = 1; i <= 6000; i++) {
                        int draws = 0;
                        for (var stack : plains.getRandomItems(params, i * 127L)) {
                            int kind = stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 0
                                    : stack.is(WinterItems.THERMAL_LINING) ? 1 : stack.is(WinterItems.HOT_WATER_BOTTLE) ? 2 : stack.is(WinterItems.WARMING_STEW) ? 3 : -1;
                            if (kind < 0) continue;
                            counts[kind]++; draws++;
                            require(stack.getCount() >= (kind == 0 ? 2 : 1) && stack.getCount() <= (kind == 0 ? 6 : kind == 3 ? 2 : 1), "supplies have exact quantity limits");
                        }
                        require(draws <= 1, "one successful optional pool draws exactly one category");
                        if (draws == 1) events++;
                    }
                    require(events > 1800 && events < 2200, "one third optional pool: " + events + "/6000");
                    double[] expected = {.5, .3, .1, .1};
                    for (int i = 0; i < 4; i++) require(Math.abs(counts[i] / (double) events - expected[i]) < .045, "category weight " + i);
                    ExtremeWinter.LOGGER.info("TEST C actual village pool samples: events={}/6000 fuel={} lining={} bottle={} stew={}", events, counts[0], counts[1], counts[2], counts[3]);
                    world.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
                    var chest = (ChestBlockEntity) world.getBlockEntity(CHEST);
                    chest.setLootTable(table("chests/village/village_plains_house"), 127);
                    chest.unpackLootTable(server.getPlayerList().getPlayers().getFirst());
                    require(chest.getLootTable() == null, "opening a real chest consumes its generation receipt");
                    for (int i = 0; i < chest.getContainerSize(); i++) stored.add(chest.getItem(i).copy());
                });
                // Test-only external data pack: it replaces this exact built-in table with a gold marker.
                try {
                    var pack = game.getWorldSave().getSaveDirectory().resolve("datapacks/winter-supplies-test");
                    Files.createDirectories(pack.resolve("data/minecraft/loot_table/chests/village"));
                    Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"isolated loot override test\",\"min_format\":[101,1],\"max_format\":[101,1]}}");
                    Files.writeString(pack.resolve("data/minecraft/loot_table/chests/village/village_plains_house.json"),
                            "{\"type\":\"minecraft:chest\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:gold_ingot\"}]}]}");
                } catch (java.io.IOException e) { throw new AssertionError("Could not write isolated test pack", e); }
                game.getServer().runOnServer(server -> {
                    server.getPackRepository().reload();
                    var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
                    require(server.getPackRepository().getAvailableIds().contains("file/winter-supplies-test"), "real external override discovered");
                    selected.add("file/winter-supplies-test"); reload[0] = server.reloadResources(selected);
                });
                waitReload(context, reload);
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    var params = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(CHEST)).create(LootContextParamSets.CHEST);
                    var loot = server.reloadableRegistries().getLootTable(table("chests/village/village_plains_house"));
                    for (int i = 1; i <= 120; i++) {
                        var result = loot.getRandomItems(params, i);
                        require(result.size() == 1 && result.getFirst().is(Items.GOLD_INGOT), "actual data pack overrides receive no winter extras");
                    }
                    var chest = (ChestBlockEntity) world.getBlockEntity(CHEST); chest.unpackLootTable(server.getPlayerList().getPlayers().getFirst());
                    for (int i = 0; i < stored.size(); i++) require(ItemStack.matches(stored.get(i), chest.getItem(i)), "old opened chest is never restocked by reload");
                    var selected = new ArrayList<>(server.getPackRepository().getSelectedIds()); selected.remove("file/winter-supplies-test");
                    ExtremeWinter.CONFIG.structureSupplies = false; reload[0] = server.reloadResources(selected);
                });
                waitReload(context, reload);
                game.getServer().runOnServer(server -> {
                    var world = server.overworld();
                    var params = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
                    for (String path : WinterSupplies.TABLES) for (int i = 1; i <= 150; i++)
                        require(server.reloadableRegistries().getLootTable(table(path)).getRandomItems(params, i).stream().noneMatch(WinterSuppliesTest::extra), "configuration disables every registered supply pool");
                });
            }
        } finally { ExtremeWinter.CONFIG.structureSupplies = previous; }
        ExtremeWinter.LOGGER.info("TEST C actual built-in chest pools, probabilities, quantities, opened chest, external data pack and disabled reload PASSED");
    }
    private static void waitReload(ClientGameTestContext context, CompletableFuture<?>[] reload) {
        for (int i = 0; i < 120 && !reload[0].isDone(); i++) context.waitTicks(2);
        require(reload[0].isDone() && !reload[0].isCompletedExceptionally(), "isolated server resource reload completes");
    }
}
