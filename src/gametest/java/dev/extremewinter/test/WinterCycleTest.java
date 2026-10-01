package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Resource-accounted survival loop; initial fixture ingredients and accelerated growth are explicit. */
public final class WinterCycleTest implements FabricClientGameTest {
    private static final BlockPos HOME = new BlockPos(8, 100, 8), TREE = new BlockPos(20, 100, 8);
    private static final BlockPos STOVE = HOME.east(2), FURNACE = HOME.west(2), CROP = HOME.north();
    private static final BlockPos SUPPLIES = new BlockPos(56, 100, 8);
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static ItemStack take(ServerPlayer p, Item item, int count) {
        require(p.getInventory().countItem(item) >= count, "inventory contains actual ingredients: " + item + " x" + count);
        var result = new ItemStack(item, count);
        for (int i = 0; i < p.getInventory().getContainerSize() && count > 0; i++) {
            var stack = p.getInventory().getItem(i);
            if (stack.is(item)) { int n = Math.min(count, stack.getCount()); p.getInventory().removeItem(i, n); count -= n; }
        }
        return result;
    }
    private static ItemStack ingredient(ServerPlayer p, Item item) { return take(p, item, 1); }
    private static void collect(ServerPlayer p, BlockPos center, double radius) {
        for (var entity : p.level().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(radius))) {
            entity.setNoPickUpDelay(); entity.playerTouch(p);
        }
    }
    private static void place(ServerPlayer p, BlockPos pos, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        require(((BlockItem) stack.getItem()).place(new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false))).consumesAction(), "native item placement: " + pos);
        if (!stack.isEmpty()) { p.getInventory().add(stack.copy()); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); }
    }
    private static ItemStack furnace(ServerPlayer p) {
        return WinterGearTest.craft(p, ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE),
                ingredient(p, Items.COBBLESTONE), ItemStack.EMPTY, ingredient(p, Items.COBBLESTONE),
                ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE));
    }
    private static ItemStack takeOutput(ServerPlayer p, BlockPos pos) {
        p.level().getBlockState(pos).useWithoutItem(p.level(), p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        require(p.containerMenu instanceof net.minecraft.world.inventory.FurnaceMenu, "native furnace menu opens");
        p.containerMenu.clicked(2, 0, ContainerInput.PICKUP, p);
        var output = p.containerMenu.getCarried().copy(); p.containerMenu.setCarried(ItemStack.EMPTY); p.closeContainer();
        require(!output.isEmpty(), "actual cooked output is taken");
        return output;
    }
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save; int[] burn = new int[1], savedBurn = new int[1], harvests = new int[1]; long[] clock = new long[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                // Explicit starting fixture: common tools, building ingredients, seeds, meat and sapling.
                int fixtureSlot = 9; p.getInventory().setSelectedSlot(8);
                for (var stack : new ItemStack[]{new ItemStack(Items.STONE_AXE), new ItemStack(Items.COBBLESTONE, 20),
                        new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.LEATHER, 3), new ItemStack(Items.COOKED_BEEF),
                        new ItemStack(Items.CARROT, 2), new ItemStack(Items.POTATO), new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.BONE_MEAL, 32)}) p.getInventory().setItem(fixtureSlot++, stack);
                for (int x = 1; x <= 60; x++) for (int z = 3; z <= 13; z++) w.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) w.setBlock(new BlockPos(x, 104, z), Blocks.GLASS.defaultBlockState(), 3);
                w.setBlock(TREE.below(), Blocks.DIRT.defaultBlockState(), 3);
                p.teleportTo(w, TREE.getX() + .5, 100, TREE.getZ() + .5, Set.of(), 0, 0, true);
                place(p, TREE, ingredient(p, Items.OAK_SAPLING));
                var meal = take(p, Items.BONE_MEAL, 32); int used = 0;
                for (; used < 32 && w.getBlockState(TREE).is(Blocks.OAK_SAPLING); used++) BoneMealItem.growCrop(meal, w, TREE);
                p.getInventory().add(meal);
                require(w.getBlockState(TREE).is(Blocks.OAK_LOG), "native sapling grows a real oak");
                p.setItemInHand(InteractionHand.MAIN_HAND, take(p, Items.STONE_AXE, 1)); int logs = 0;
                for (int y = 0; y < 12; y++) if (w.getBlockState(TREE.above(y)).is(Blocks.OAK_LOG)) {
                    require(p.gameMode.destroyBlock(TREE.above(y)), "native survival tree harvest"); logs++;
                }
                collect(p, TREE, 16);
                require(logs >= 4 && p.getInventory().countItem(Items.OAK_LOG) == logs, "tree drops are collected exactly");
                p.getInventory().add(p.getMainHandItem().copy()); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.teleportTo(w, HOME.getX() + .5, 100, HOME.getZ() + .5, Set.of(), 0, 0, true);
                p.getInventory().add(WinterGearTest.craft(p, ingredient(p, Items.OAK_LOG)));
                p.getInventory().add(WinterGearTest.craft(p, ingredient(p, Items.OAK_LOG)));
                place(p, FURNACE, furnace(p));
                var stoveItem = WinterGearTest.craft(p, furnace(p), ingredient(p, Items.IRON_INGOT), ingredient(p, Items.IRON_INGOT),
                        ingredient(p, Items.IRON_INGOT), ingredient(p, Items.IRON_INGOT), ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE), ingredient(p, Items.COBBLESTONE));
                place(p, STOVE, stoveItem);
                var f = (AbstractFurnaceBlockEntity) w.getBlockEntity(FURNACE);
                f.setItem(0, take(p, Items.OAK_LOG, 2)); f.setItem(1, take(p, Items.OAK_PLANKS, 3));
                TemperatureData.set(p, 70);
                ExtremeWinter.LOGGER.info("TEST D cycle native tree logs={} boneMealAttempts={} charcoal input=2 logs fuel=3 crafted planks", logs, used);
            });
            context.waitTicks(410);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                var charcoal = takeOutput(p, FURNACE);
                require(charcoal.is(Items.CHARCOAL) && charcoal.getCount() == 2, "two harvested logs smelt to two charcoal over actual ticks");
                p.getInventory().add(charcoal);
                ((HeatingStoveBlockEntity) w.getBlockEntity(STOVE)).setItem(0, take(p, Items.CHARCOAL, 1));
                w.setBlock(CROP.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 3);
                place(p, CROP, ingredient(p, Items.POTATO));
                w.setBlock(CROP.west().below(), Blocks.WATER.defaultBlockState(), 3);
                var bag = WinterGearTest.craft(p, ingredient(p, Items.LEATHER), ingredient(p, Items.LEATHER), ingredient(p, Items.LEATHER), ingredient(p, Items.IRON_INGOT));
                p.setItemInHand(InteractionHand.MAIN_HAND, bag); var kettle = HOME.south(2);
                w.setBlock(kettle, Blocks.WATER_CAULDRON.defaultBlockState(), 3);
                p.gameMode.useItemOn(p, w, bag, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(kettle), Direction.UP, kettle, false));
                require(bag.getOrDefault(WinterGear.FILLED, false), "crafted bag actually fills");
            });
            context.waitTicks(220);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                var stove = (HeatingStoveBlockEntity) w.getBlockEntity(STOVE); burn[0] = stove.remainingTicks();
                require(burn[0] > 9300 && burn[0] < 9600 && stove.getItem(0).isEmpty(), "one generated charcoal supplies eight minutes, consumed once");
                require(WinterGear.remaining(p.getMainHandItem(), WinterWorldState.get(w).elapsedTicks()) > 14000, "actual stove charges carried bottle");
                require(TemperatureData.get(p) > 70, "home really restores warmth");
                require(WinterFarming.greenhouse(w, CROP), "actual roof/light/source meet greenhouse conditions");
                var heat = new HeatSources(ExtremeWinter.CONFIG);
                require(heat.strengthAt(w, STOVE.east(7)) == 0, "stove does not heat beyond six-block radius");
                var random = RandomSource.create(20261001); int attempts = 0;
                while (!((CropBlock) Blocks.POTATOES).isMaxAge(w.getBlockState(CROP)) && attempts++ < 2000) w.getBlockState(CROP).randomTick(w, CROP, random);
                require(((CropBlock) Blocks.POTATOES).isMaxAge(w.getBlockState(CROP)), "native randomized growth produces a mature crop");
                p.getInventory().add(p.getMainHandItem().copy()); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                require(p.gameMode.destroyBlock(CROP), "native mature greenhouse harvest"); collect(p, CROP, 4);
                require(p.getInventory().countItem(Items.POTATO) >= 2 && p.getAttachedOrCreate(WinterTasks.HARVESTS) > 0, "produce becomes usable food and recorded progress");
                harvests[0] = p.getAttachedOrCreate(WinterTasks.HARVESTS);
                place(p, CROP, ingredient(p, Items.POTATO));
                var f = (AbstractFurnaceBlockEntity) w.getBlockEntity(FURNACE); f.setItem(0, ingredient(p, Items.POTATO)); f.setItem(1, ingredient(p, Items.CHARCOAL));
                ExtremeWinter.LOGGER.info("TEST D cycle crop light={} heat={} attempts={} actualProduce={} warmth={} stoveRemaining={}", w.getRawBrightness(CROP, 0), heat.strengthAt(w, CROP), attempts, harvests[0], TemperatureData.get(p), burn[0]);
            });
            context.waitTicks(200);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst(); var stove = (HeatingStoveBlockEntity) w.getBlockEntity(STOVE);
                require(burn[0] - stove.remainingTicks() == 200, "exact native 200-tick fuel consumption");
                var potato = takeOutput(p, FURNACE); require(potato.is(Items.BAKED_POTATO), "actual harvested potato cooks");
                var bowl = WinterGearTest.craft(p, ingredient(p, Items.OAK_PLANKS), ItemStack.EMPTY, ingredient(p, Items.OAK_PLANKS), ItemStack.EMPTY, ingredient(p, Items.OAK_PLANKS));
                p.getInventory().add(bowl);
                var stew = WinterGearTest.craft(p, ingredient(p, Items.BOWL), potato, ingredient(p, Items.COOKED_BEEF), ingredient(p, Items.CARROT));
                require(stew.is(WinterItems.WARMING_STEW), "harvested and cooked food crafts warming stew");
                p.setItemInHand(InteractionHand.MAIN_HAND, stew); p.getFoodData().setFoodLevel(10); p.getFoodData().setSaturation(0);
                p.inventoryMenu.broadcastChanges();
                p.gameMode.useItem(p, w, stew, InteractionHand.MAIN_HAND);
            });
            context.setScreen(() -> null); context.waitTicks(2);
            context.runOnClient(c -> c.options.keyUse.setDown(true)); context.waitTicks(35); context.runOnClient(c -> c.options.keyUse.setDown(false));
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level(); p.stopUsingItem();
                require(p.getFoodData().getFoodLevel() == 16 && p.getAttachedOrCreate(WinterGear.STEW_UNTIL) > WinterWorldState.get(w).elapsedTicks(), "real stew eating grants travel effect: hunger=" + p.getFoodData().getFoodLevel() + " hand=" + p.getMainHandItem() + " expiry=" + p.getAttachedOrCreate(WinterGear.STEW_UNTIL));
                var table = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/village/village_plains_house"));
                var params = new LootParams.Builder(w).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(SUPPLIES)).create(LootContextParamSets.CHEST);
                long seed = 0;
                for (long candidate = 1; candidate <= 6000 && seed == 0; candidate++) if (server.reloadableRegistries().getLootTable(table).getRandomItems(params, candidate).stream().anyMatch(s -> s.is(Items.COAL) || s.is(Items.CHARCOAL))) seed = candidate;
                require(seed != 0, "fixed optional native supply pool has a reproducible fuel seed");
                w.setBlock(SUPPLIES, Blocks.CHEST.defaultBlockState(), 3); ((ChestBlockEntity) w.getBlockEntity(SUPPLIES)).setLootTable(table, seed);
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ExtremeWinter.LOGGER.info("TEST D cycle supply fixture native village table seed={} destinationDistance=48", seed);
            });
            // Scripted one-block travel steps process exposure and portable heat on every running tick.
            for (int step = 1; step <= 48; step++) {
                int x = HOME.getX() + step;
                game.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setPos(x + .5, 100, 8.5); });
                context.runOnClient(c -> c.player.setPos(x + .5, 100, 8.5));
                context.waitTicks(1);
            }
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level();
                require(new HeatSources(ExtremeWinter.CONFIG).strength(p) == 0 && WinterGear.portableGain(p) == .03, "travel is outside stove range with active carried bottle");
                w.getBlockState(SUPPLIES).useWithoutItem(w, p, new BlockHitResult(Vec3.atCenterOf(SUPPLIES), Direction.UP, SUPPLIES, false));
                var chest = (ChestBlockEntity) w.getBlockEntity(SUPPLIES);
                require(chest.getLootTable() == null, "real chest opening consumes its generation receipt");
                for (int i = 0; i < 27; i++) if (chest.getItem(i).is(Items.COAL) || chest.getItem(i).is(Items.CHARCOAL)) p.containerMenu.clicked(i, 0, ContainerInput.QUICK_MOVE, p);
                p.closeContainer();
                require(p.getInventory().countItem(Items.COAL) + p.getInventory().countItem(Items.CHARCOAL) > 0, "native chest transfer refills expedition fuel");
            });
            for (int step = 47; step >= 0; step--) {
                int x = HOME.getX() + step;
                game.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.setPos(x + .5, 100, 8.5); });
                context.runOnClient(c -> c.player.setPos(x + .5, 100, 8.5)); context.waitTicks(1);
            }
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level(); var stove = (HeatingStoveBlockEntity) w.getBlockEntity(STOVE);
                Item fuel = p.getInventory().countItem(Items.COAL) > 0 ? Items.COAL : Items.CHARCOAL;
                stove.setItem(0, take(p, fuel, 1)); p.setGameMode(GameType.SPECTATOR);
                savedBurn[0] = stove.remainingTicks(); clock[0] = WinterWorldState.get(w).elapsedTicks();
                ExtremeWinter.LOGGER.info("TEST D cycle return fuel={} stoveRemaining={} elapsedTicks={} harvestReceipt={}", fuel, savedBurn[0], clock[0], harvests[0]);
            });
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level(); var stove = (HeatingStoveBlockEntity) w.getBlockEntity(STOVE);
                // Loaded furnaces tick even with a spectator; compare a narrow running interval, not wall-clock time.
                require(stove.remainingTicks() <= savedBurn[0] && savedBurn[0] - stove.remainingTicks() < 80, "saved running fuel resumes without offline catch-up");
                require(!stove.getItem(0).isEmpty() && w.getBlockState(CROP).is(Blocks.POTATOES), "replanted crop and expedition fuel survive save/rejoin");
                require(p.getAttachedOrCreate(WinterTasks.HARVESTS) == harvests[0] && WinterWorldState.get(w).elapsedTicks() == clock[0], "harvest receipt and winter clock survive save/rejoin");
                p.setGameMode(GameType.SURVIVAL); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WinterItems.SURVIVAL_MANUAL));
            });
            renderManual(context, game);
        }
        ExtremeWinter.LOGGER.info("TEST D actual tree/charcoal/stove/greenhouse/cooked stew/travel/native supplies/save and twelve manual pages PASSED");
    }
    private static void renderManual(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext game) {
        String[] previous = new String[1]; context.runOnClient(c -> previous[0] = c.options.languageCode);
        try {
            for (String language : new String[]{"en_us", "zh_cn"}) {
                context.runOnClient(c -> { c.options.languageCode = language; c.getLanguageManager().setSelected(language); c.getLanguageManager().onResourceManagerReload(c.getResourceManager()); });
                game.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND); });
                context.waitTicks(2);
                for (int page = 0; page < 6; page++) {
                    int index = page;
                    context.runOnClient(c -> {
                        require(c.screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen, "native manual reading screen");
                        ((net.minecraft.client.gui.screens.inventory.BookViewScreen) c.screen).setPage(index);
                        int lines = c.font.split(net.minecraft.network.chat.Component.translatable("manual.extreme_winter.page" + (index + 1)), 114).size();
                        require(lines * c.font.lineHeight <= 128, "manual fits native page: " + language + " page " + (index + 1) + " lines=" + lines);
                    });
                    context.waitTicks(1); context.takeScreenshot("beginner-D-manual-" + language + "-" + (page + 1));
                }
                context.setScreen(() -> null);
            }
        } finally {
            context.runOnClient(c -> { c.options.languageCode = previous[0]; c.getLanguageManager().setSelected(previous[0]); c.getLanguageManager().onResourceManagerReload(c.getResourceManager()); });
        }
    }
}
