package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.survival.StarterSupplies;
import dev.extremewinter.temperature.HeatItems;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;

public final class StarterAndHeatItemsTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        boolean oldfurnaceWeathering=ExtremeWinter.CONFIG.furnaceWeathering; ExtremeWinter.CONFIG.furnaceWeathering=true;
        try {
        // Two fresh saves in one Minecraft process, followed by a reopen of the first save.
        TestWorldSave first = null;
        for (int index = 0; index < 2; index++) {
            try (var game = context.worldBuilder().create()) {
                if (first == null) first = game.getWorldSave();
                game.getClientLevel().waitForChunksRender();
                game.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    require(player.getInventory().countItem(Items.CAMPFIRE) == 1, "every fresh save receives one campfire");
                    var position = player.position();
                    var spawn = server.overworld().getRespawnData().pos();
                    StarterSupplies.onJoin(player);
                    require(player.position().equals(position) && server.overworld().getRespawnData().pos().equals(spawn),
                            "starter kit does not teleport or change vanilla spawn");
                    require(player.getInventory().countItem(Items.CAMPFIRE) == 1, "repeated join does not duplicate kit");
                    player.getInventory().clearContent();
                    player.getInventory().setChanged();
                });
            }
        }
        try (var game = first.open()) {
            game.getClientLevel().waitForChunksRender();
            game.getServer().runOnServer(server -> require(
                    server.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.CAMPFIRE) == 0,
                    "reopened save does not replenish the starter kit"));
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                TemperatureData.set(player, 80);
                for (int x = 16; x <= 30; x++) for (int z = -3; z <= 3; z++) {
                    world.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                    world.setBlock(new BlockPos(x, 104, z), Blocks.GLASS.defaultBlockState(), 3);
                }
                player.teleportTo(world, 20.5, 100, 0.5, Set.of(), 0, 0, true);
                var camp = new BlockPos(22, 100, 0);
                world.setBlock(camp, Blocks.CAMPFIRE.defaultBlockState(), 3);
                var entity = (CampfireBlockEntity) world.getBlockEntity(camp);
                entity.getItems().set(0, new ItemStack(Items.BEEF));
                HeatItems.writeExposure(entity, 100);
                player.setShiftKeyDown(true);
                var hit = new BlockHitResult(Vec3.atCenterOf(camp), Direction.UP, camp, false);
                var action = UseBlockCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND, hit);
                require(action.consumesAction() && world.getBlockState(camp).isAir(), "empty-hand sneaking picks up campfire");
                player.setShiftKeyDown(false);
                var returned = player.getInventory().getItem(0);
                require(returned.is(Items.CAMPFIRE) && HeatItems.elapsed(returned) == 100,
                        "picked-up campfire keeps partially used clock");
                int beef = world.getEntitiesOfClass(ItemEntity.class, new AABB(camp).inflate(2),
                        item -> item.getItem().is(Items.BEEF)).stream().mapToInt(item -> item.getItem().getCount()).sum();
                require(beef == 1, "pickup preserves cooking item exactly once");
                var furnace = new BlockPos(25, 100, 0);
                world.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
                HeatItems.writeExposure(world.getBlockEntity(furnace), 200);
                var drops = Block.getDrops(world.getBlockState(furnace), world, furnace,
                        world.getBlockEntity(furnace), player, new ItemStack(Items.IRON_PICKAXE));
                var dropped = drops.stream().filter(stack -> stack.is(Items.FURNACE)).findFirst().orElseThrow();
                require(HeatItems.elapsed(dropped) == 200, "ordinary furnace drop preserves clock");
                var destination = new BlockPos(27, 100, 0);
                player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
                var support = destination.below();
                var placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, dropped,
                        new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false));
                require(((BlockItem) Items.FURNACE).place(placement).consumesAction(), "real BlockItem placement succeeds");
                require(world.getBlockState(destination).is(Blocks.FURNACE)
                        && HeatItems.elapsed(world.getBlockEntity(destination)) == 200,
                        "placed furnace retains its clock instead of becoming full");
                // A completely depleted campfire is placed extinguished, with no free burning tick.
                var empty = new ItemStack(Items.CAMPFIRE);
                empty.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.campfireExposureSeconds);
                player.setItemInHand(InteractionHand.MAIN_HAND, empty);
                var emptySupport = new BlockPos(29, 99, 0);
                require(((BlockItem) Items.CAMPFIRE).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, empty,
                        new BlockHitResult(Vec3.atCenterOf(emptySupport).add(0, 0.5, 0), Direction.UP, emptySupport, false))).consumesAction(),
                        "depleted campfire can be placed");
                require(!world.getBlockState(emptySupport.above()).getValue(BlockStateProperties.LIT), "depleted campfire is immediately unlit");
                player.getInventory().clearContent();
                var recovering = new ItemStack(Items.CAMPFIRE);
                recovering.set(HeatItems.EXPOSURE, 100);
                player.getInventory().setItem(0, recovering);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                int elapsed = HeatItems.elapsed(player.getInventory().getItem(0));
                require(elapsed == 60, "two real seconds recover forty of six hundred exposure seconds: " + elapsed);
                for (var item : new net.minecraft.world.item.Item[]{Items.CAMPFIRE, Items.SOUL_CAMPFIRE,
                        Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER}) {
                    var stack = new ItemStack(item);
                    int limit = HeatItems.limit(item, ExtremeWinter.CONFIG);
                    require(!stack.isBarVisible(), "unused heat source has no persistent bar");
                    stack.set(HeatItems.EXPOSURE, limit);
                    require(stack.isBarVisible() && stack.getBarWidth() == 0, "depleted source displays empty bar");
                    for (int second = 0; second < 30; second++) HeatItems.recover(stack, ExtremeWinter.CONFIG);
                    require(HeatItems.elapsed(stack) == 0 && !stack.isBarVisible() && stack.getBarWidth() == 13,
                            "fully recovered heat source hides its bar");
                }
                player.getInventory().clearContent();
                for (int slot = 0; slot < 4; slot++) {
                    var stack = new ItemStack(Items.CAMPFIRE);
                    int fixtureElapsed = new int[]{0, 300, 540, 600}[slot];
                    stack.set(HeatItems.EXPOSURE, fixtureElapsed);
                    player.getInventory().setItem(slot, stack);
                }
                var items = new net.minecraft.world.item.Item[]{Items.SOUL_CAMPFIRE, Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER};
                for (int slot = 0; slot < items.length; slot++) {
                    var stack = new ItemStack(items[slot]);
                    stack.set(HeatItems.EXPOSURE, HeatItems.limit(items[slot], ExtremeWinter.CONFIG) / 2);
                    player.getInventory().setItem(slot + 4, stack);
                }
                var sword = new ItemStack(Items.STONE_SWORD);
                sword.setDamageValue(80);
                player.getInventory().setItem(8, sword);
                require(sword.isBarVisible(), "ordinary weapon durability is retained");
                player.getInventory().setChanged();
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                require(client.player.getInventory().getItem(1).getBarWidth() >= 6
                        && client.player.getInventory().getItem(1).getBarWidth() <= 7, "item component and bar sync to client");
                client.setScreen(new InventoryScreen(client.player));
            });
            context.takeScreenshot("heat-charge-inventory-" + System.getProperty("winter.test.profile", "A"));
            context.setScreen(() -> null);
            game.getServer().runOnServer(server -> {
                var stored = new ItemStack(Items.CAMPFIRE);
                stored.set(HeatItems.EXPOSURE, 80);
                server.getPlayerList().getPlayers().getFirst().getInventory().setItem(9, stored);
            });
        }
        try (var game = first.open()) {
            game.getServer().runOnServer(server -> {
                var stored = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(9);
                require(stored.is(Items.CAMPFIRE) && HeatItems.elapsed(stored) > 0 && HeatItems.elapsed(stored) <= 80,
                        "partially charged inventory item survives save/reopen without becoming full");
            });
        }
        } finally {
            ExtremeWinter.CONFIG.furnaceWeathering=oldfurnaceWeathering;
        }
        ExtremeWinter.LOGGER.info("TEST vanilla spawn, two new saves, one starter campfire, portable clocks and inventory bars PASSED");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
