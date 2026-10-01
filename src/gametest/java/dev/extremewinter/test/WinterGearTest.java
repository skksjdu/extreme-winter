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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.trim.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class WinterGearTest implements FabricClientGameTest {
    private static void require(boolean b, String message) { if (!b) throw new AssertionError(message); }
    public static ItemStack craft(ServerPlayer p, ItemStack... stacks) {
        var menu = new CraftingMenu(41, p.getInventory(), ContainerLevelAccess.create(p.level(), p.blockPosition()));
        p.containerMenu = menu;
        for (int i = 0; i < stacks.length; i++) menu.getSlot(i + 1).set(stacks[i]);
        menu.slotsChanged(menu.getSlot(1).container);
        require(!menu.getSlot(0).getItem().isEmpty(), "actual crafting menu has a result");
        menu.clicked(0, 0, ContainerInput.PICKUP, p);
        var result = menu.getCarried().copy();
        menu.setCarried(ItemStack.EMPTY);
        p.containerMenu = p.inventoryMenu;
        return result;
    }
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        long[] expiry = new long[1], clock = new long[1], stew = new long[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var world = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                require(p.getInventory().countItem(WinterItems.SURVIVAL_MANUAL) == 1, "new save grants one independent manual");
                StarterSupplies.onJoin(p);
                require(p.getInventory().countItem(WinterItems.SURVIVAL_MANUAL) == 1, "join does not duplicate manual");
                require(p.getInventory().countItem(Items.CAMPFIRE) == 1, "manual never resets starter receipt");
                p.getInventory().clearContent();
                for (int x = 16; x <= 28; x++) for (int z = -4; z <= 4; z++) {
                    world.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                    world.setBlock(new BlockPos(x, 104, z), Blocks.GLASS.defaultBlockState(), 3);
                }
                p.teleportTo(world, 20.5, 100, .5, Set.of(), 0, 0, true);
                var original = new ItemStack(Items.LEATHER_CHESTPLATE);
                original.setDamageValue(40);
                original.set(DataComponents.CUSTOM_NAME, Component.literal("Saved coat"));
                original.set(DataComponents.DYED_COLOR, new DyedItemColor(0x347876));
                original.set(DataComponents.TRIM, new ArmorTrim(server.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.IRON),
                        server.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY)));
                original.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 2);
                original.set(HeatItems.EXPOSURE, 73);
                var lined = craft(p, original.copy(), new ItemStack(WinterItems.THERMAL_LINING));
                var expected = original.copy(); expected.set(WinterGear.INSULATION, true);
                require(ItemStack.isSameItemSameComponents(expected, lined), "lining copies name, damage, dye, trim, enchants and unrelated components");
                var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(lined, new ItemStack(WinterItems.THERMAL_LINING)));
                require(!ThermalLiningRecipe.INSTANCE.matches(input, world), "repeat lining has no recipe");
                var repaired = craft(p, lined.copy(), damaged(Items.LEATHER_CHESTPLATE, 30));
                require(WinterGear.lined(repaired) && repaired.getDamageValue() < 40, "actual vanilla crafting repair retains lining");
                p.giveExperienceLevels(30);
                var anvil = new AnvilMenu(42, p.getInventory(), ContainerLevelAccess.create(world, p.blockPosition()));
                p.containerMenu = anvil;
                anvil.getSlot(0).set(lined.copy()); anvil.getSlot(1).set(new ItemStack(Items.LEATHER)); anvil.createResult();
                require(WinterGear.lined(anvil.getSlot(2).getItem()) && anvil.getSlot(2).getItem().getDamageValue() < 40, "anvil material repair retains lining");
                anvil.clicked(2, 0, ContainerInput.PICKUP, p);
                require(WinterGear.lined(anvil.getCarried()), "anvil result can really be taken");
                anvil.setCarried(ItemStack.EMPTY); p.containerMenu = p.inventoryMenu;
                var combine = new AnvilMenu(44, p.getInventory(), ContainerLevelAccess.create(world, p.blockPosition()));
                combine.getSlot(0).set(damaged(Items.LEATHER_CHESTPLATE, 50)); combine.getSlot(1).set(lined.copy()); combine.createResult();
                require(WinterGear.lined(combine.getSlot(2).getItem()), "anvil combine keeps lining from either input orientation");
                var diamond = craft(p, damaged(Items.DIAMOND_CHESTPLATE, 100), new ItemStack(WinterItems.THERMAL_LINING));
                var smith = new SmithingMenu(43, p.getInventory(), ContainerLevelAccess.create(world, p.blockPosition()));
                p.containerMenu = smith;
                smith.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                smith.getSlot(1).set(diamond); smith.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT)); smith.createResult();
                require(smith.getSlot(3).getItem().is(Items.NETHERITE_CHESTPLATE) && WinterGear.lined(smith.getSlot(3).getItem()), "netherite recipe preserves lining");
                smith.clicked(3, 0, ContainerInput.PICKUP, p);
                require(smith.getCarried().is(Items.NETHERITE_CHESTPLATE) && WinterGear.lined(smith.getCarried()), "actual smithing take retains lining");
                smith.setCarried(ItemStack.EMPTY); p.containerMenu = p.inventoryMenu;
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, lined.copy());
                require(Math.abs(WinterGear.airMultiplier(p) - .80) < 1e-9, "lined leather chest weighs forty percent: .15+.35 times .4");
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
                var bag = craft(p, new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER), new ItemStack(Items.IRON_INGOT));
                require(bag.is(WinterItems.HOT_WATER_BOTTLE) && bag.getMaxStackSize() == 1 && !bag.getOrDefault(WinterGear.FILLED, false), "bottle recipe produces single empty bottle");
                p.setItemInHand(InteractionHand.MAIN_HAND, bag);
                var kettle = new BlockPos(20, 100, 2);
                world.setBlock(kettle, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2), 3);
                var hit = new BlockHitResult(Vec3.atCenterOf(kettle), Direction.UP, kettle, false);
                p.gameMode.useItemOn(p, world, bag, InteractionHand.MAIN_HAND, hit);
                require(bag.getOrDefault(WinterGear.FILLED, false) && world.getBlockState(kettle).getValue(LayeredCauldronBlock.LEVEL) == 1, "real water cauldron interaction consumes exactly one layer");
                world.setBlock(new BlockPos(22, 100, 0), Blocks.CAMPFIRE.defaultBlockState(), 3);
                require(HotWaterBottleItem.canCharge(p), "nearby lit campfire is actually visible");
            });
            context.waitTicks(100);
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                require(p.getMainHandItem().getOrDefault(WinterGear.CHARGE, 0) >= 90, "real five seconds partially charge bottle");
                p.teleportTo(server.overworld(), 17.5, 100, .5, Set.of(), 0, 0, true);
            });
            context.waitTicks(2);
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                require(p.getMainHandItem().getOrDefault(WinterGear.CHARGE, -1) == 0, "leaving immediately resets uninterrupted progress");
                p.teleportTo(server.overworld(), 20.5, 100, .5, Set.of(), 0, 0, true);
            });
            context.waitTicks(205);
            game.getServer().runOnServer(server -> {
                var world = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                var bag = p.getMainHandItem(); long now = WinterWorldState.get(world).elapsedTicks();
                require(WinterGear.remaining(bag, now) >= WinterGear.BOTTLE_DURATION - 20, "ten real seconds fill twelve running minutes of heat");
                expiry[0] = bag.get(WinterGear.EXPIRY);
                var chestPos = new BlockPos(24, 100, 2);
                world.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
                var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) world.getBlockEntity(chestPos);
                chest.setItem(0, bag.copy()); p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                require(WinterGear.portableGain(p) == 0, "stored bottle does not warm player");
                p.getInventory().setItem(9, bag.copy()); p.getInventory().setItem(10, bag.copy());
                require(WinterGear.portableGain(p) == .03, "multiple charged bottles give one .03 gain");
                var dropped = p.drop(bag.copy(), false);
                require(dropped.getItem().get(WinterGear.EXPIRY) == expiry[0], "dropping keeps absolute expiry");
                world.setBlock(new BlockPos(21, 101, 0), Blocks.STONE.defaultBlockState(), 3);
                require(!HotWaterBottleItem.canCharge(p), "solid wall blocks charging");
                world.setBlock(new BlockPos(21, 101, 0), Blocks.AIR.defaultBlockState(), 3);
                var source = new BlockPos(20, 99, -2); world.setBlock(source, Blocks.WATER.defaultBlockState(), 3);
                p.teleportTo(world, 20.5, 100, -1.5, Set.of(), 0, 90, true);
                var empty = new ItemStack(WinterItems.HOT_WATER_BOTTLE); p.setItemInHand(InteractionHand.MAIN_HAND, empty);
                p.gameMode.useItem(p, world, empty, InteractionHand.MAIN_HAND);
                require(empty.getOrDefault(WinterGear.FILLED, false) && world.getBlockState(source).is(Blocks.WATER), "source water use fills without removing source");
                p.teleportTo(world, 20.5, 100, .5, Set.of(), 0, 0, true);
                var food = craft(p, new ItemStack(Items.BOWL), new ItemStack(Items.BAKED_POTATO), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.CARROT));
                require(food.is(WinterItems.WARMING_STEW) && food.getMaxStackSize() == 16, "stew recipe and stack limit");
                food.setCount(2); p.setItemInHand(InteractionHand.MAIN_HAND, food);
                p.getFoodData().setFoodLevel(10); p.getFoodData().setSaturation(0); TemperatureData.set(p, 40);
                p.gameMode.useItem(p, world, food, InteractionHand.MAIN_HAND);
                require(p.isUsingItem(), "hungry player starts ordinary eating");
            });
            context.runOnClient(client -> client.options.keyUse.setDown(true));
            context.waitTicks(35);
            context.runOnClient(client -> client.options.keyUse.setDown(false));
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                require(p.getFoodData().getFoodLevel() == 16 && Math.abs(p.getFoodData().getSaturationLevel() - 4.8) < .001, "real eating grants six hunger and 4.8 saturation: hunger=" + p.getFoodData().getFoodLevel() + " saturation=" + p.getFoodData().getSaturationLevel());
                require(p.getMainHandItem().getCount() == 1 && p.getInventory().countItem(Items.BOWL) == 1, "stacked stew returns one bowl without replacing the remaining stew");
                require(TemperatureData.get(p) >= 50, "actual eating warms by ten");
                stew[0] = p.getAttachedOrCreate(WinterGear.STEW_UNTIL);
                require(stew[0] - WinterWorldState.get(p.level()).elapsedTicks() >= 5990, "stew effect is five running minutes");
                p.stopUsingItem();
                p.getFoodData().setFoodLevel(20); p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
                require(!p.isUsingItem(), "full hunger cannot consume ordinary stew");
                p.getFoodData().setFoodLevel(10); p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
            });
            context.runOnClient(client -> client.options.keyUse.setDown(true));
            context.waitTicks(35);
            context.runOnClient(client -> client.options.keyUse.setDown(false));
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                require(p.getMainHandItem().is(Items.BOWL) && p.getInventory().countItem(Items.BOWL) == 2, "last stew converts its hand stack to one bowl");
                long now = WinterWorldState.get(p.level()).elapsedTicks();
                require(p.getAttachedOrCreate(WinterGear.STEW_UNTIL) > stew[0]
                        && p.getAttachedOrCreate(WinterGear.STEW_UNTIL) <= now + 6000, "second serving refreshes without duration stacking");
                var manual = craft(p, new ItemStack(Items.BOOK), new ItemStack(Items.WHITE_WOOL));
                require(manual.is(WinterItems.SURVIVAL_MANUAL) && manual.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == 6, "lost manual is craftable and has six reading pages");
                p.setItemInHand(InteractionHand.MAIN_HAND, manual);
                p.gameMode.useItem(p, p.level(), manual, InteractionHand.MAIN_HAND);
            });
            context.waitTicks(2);
            context.runOnClient(client -> require(client.screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen, "manual opens native book reading screen"));
            context.takeScreenshot("beginner-C-manual"); context.setScreen(() -> null);
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var world = p.level();
                var meter = craft(p, new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.GLASS), new ItemStack(Items.REDSTONE));
                require(meter.is(WinterItems.WARMTH_METER), "warmth meter recipe works");
                p.setItemInHand(InteractionHand.MAIN_HAND, meter); p.gameMode.useItem(p, world, meter, InteractionHand.MAIN_HAND);
                var instrument = craft(p, meter, new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS));
                p.setItemInHand(InteractionHand.MAIN_HAND, instrument);
                var support = new BlockPos(26, 99, 0);
                require(((BlockItem) instrument.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(p, InteractionHand.MAIN_HAND, instrument,
                        new BlockHitResult(Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false))).consumesAction(), "instrument places as a real block item");
                require(world.getBlockState(support.above()).is(WinterItems.WEATHER_INSTRUMENT), "instrument block exists");
                world.getBlockState(support.above()).useWithoutItem(world, p, new BlockHitResult(Vec3.atCenterOf(support.above()), Direction.UP, support.above(), false));
                p.getInventory().clearContent();
                p.getInventory().setItem(9, ((net.minecraft.world.level.block.entity.ChestBlockEntity) world.getBlockEntity(new BlockPos(24, 100, 2))).getItem(0).copy());
                p.setGameMode(GameType.SPECTATOR); clock[0] = WinterWorldState.get(world).elapsedTicks();
            });
            // Settle the final inventory/chat packets and spectator transition before closing the world.
            context.setScreen(() -> null);
            context.waitTicks(2);
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst(); var state = WinterWorldState.get(p.level());
                require(state.elapsedTicks() == clock[0], "offline does not consume bottle heat");
                var bag = p.getInventory().getItem(9);
                require(bag.get(WinterGear.EXPIRY) == expiry[0] && bag.getOrDefault(WinterGear.CHARGE, -1) == 0, "rejoin retains expiry and resets partial continuous charging");
                p.setGameMode(GameType.SURVIVAL); StarterSupplies.onJoin(p);
                require(p.getInventory().countItem(WinterItems.SURVIVAL_MANUAL) == 0, "lost guide is never silently granted again");
                var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) p.level().getBlockEntity(new BlockPos(24, 100, 2));
                require(chest.getItem(0).get(WinterGear.EXPIRY) == expiry[0], "stored bottle keeps same deadline on rejoin");
                state.setElapsedTicks(expiry[0]);
                require(WinterGear.remaining(bag, state.elapsedTicks()) == 0 && WinterGear.portableGain(p) == 0, "expiry boundary removes heat even after storage");
            });
        }
        ExtremeWinter.LOGGER.info("TEST C armor component preservation, crafting/anvil/smithing, real charging, food, guide, instruments and rejoin PASSED");
    }
    private static ItemStack damaged(Item item, int damage) { var result = new ItemStack(item); result.setDamageValue(damage); return result; }
}
