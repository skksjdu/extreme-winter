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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class StarterAndHeatItemsTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        // Two fresh saves in one Minecraft process, followed by a reopen of the first save.
        TestWorldSave first = null;
        for (int index = 0; index < 2; index++) {
            try (var game = context.worldBuilder().create()) {
                if (first == null) first = game.getWorldSave();
                game.getClientWorld().waitForChunksRender();
                game.getServer().runOnServer(server -> {
                    var player = server.getPlayerManager().getPlayerList().getFirst();
                    require(player.getInventory().count(Items.CAMPFIRE) == 1, "every fresh save receives one campfire");
                    var position = player.getPos();
                    var spawn = server.getOverworld().getSpawnPos();
                    StarterSupplies.onJoin(player);
                    require(player.getPos().equals(position) && server.getOverworld().getSpawnPos().equals(spawn),
                            "starter kit does not teleport or change vanilla spawn");
                    require(player.getInventory().count(Items.CAMPFIRE) == 1, "repeated join does not duplicate kit");
                    player.getInventory().clear();
                    player.getInventory().markDirty();
                });
            }
        }
        try (var game = first.open()) {
            game.getClientWorld().waitForChunksRender();
            game.getServer().runOnServer(server -> require(
                    server.getPlayerManager().getPlayerList().getFirst().getInventory().count(Items.CAMPFIRE) == 0,
                    "reopened save does not replenish the starter kit"));
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.changeGameMode(GameMode.SURVIVAL);
                TemperatureData.set(player, 80);
                for (int x = 16; x <= 30; x++) for (int z = -3; z <= 3; z++) {
                    world.setBlockState(new BlockPos(x, 99, z), Blocks.STONE.getDefaultState());
                    world.setBlockState(new BlockPos(x, 104, z), Blocks.GLASS.getDefaultState());
                }
                player.teleport(world, 20.5, 100, 0.5, Set.of(), 0, 0, true);
                var camp = new BlockPos(22, 100, 0);
                world.setBlockState(camp, Blocks.CAMPFIRE.getDefaultState());
                var entity = (CampfireBlockEntity) world.getBlockEntity(camp);
                entity.getItemsBeingCooked().set(0, new ItemStack(Items.BEEF));
                HeatItems.writeExposure(entity, 100);
                player.setSneaking(true);
                var hit = new BlockHitResult(Vec3d.ofCenter(camp), Direction.UP, camp, false);
                var action = UseBlockCallback.EVENT.invoker().interact(player, world, Hand.MAIN_HAND, hit);
                require(action.isAccepted() && world.getBlockState(camp).isAir(), "empty-hand sneaking picks up campfire");
                player.setSneaking(false);
                var returned = player.getInventory().getStack(0);
                require(returned.isOf(Items.CAMPFIRE) && HeatItems.elapsed(returned) == 100,
                        "picked-up campfire keeps partially used clock");
                int beef = world.getEntitiesByClass(ItemEntity.class, new Box(camp).expand(2),
                        item -> item.getStack().isOf(Items.BEEF)).stream().mapToInt(item -> item.getStack().getCount()).sum();
                require(beef == 1, "pickup preserves cooking item exactly once");
                var furnace = new BlockPos(25, 100, 0);
                world.setBlockState(furnace, Blocks.FURNACE.getDefaultState());
                HeatItems.writeExposure(world.getBlockEntity(furnace), 200);
                var drops = Block.getDroppedStacks(world.getBlockState(furnace), world, furnace,
                        world.getBlockEntity(furnace), player, new ItemStack(Items.IRON_PICKAXE));
                var dropped = drops.stream().filter(stack -> stack.isOf(Items.FURNACE)).findFirst().orElseThrow();
                require(HeatItems.elapsed(dropped) == 200, "ordinary furnace drop preserves clock");
                var destination = new BlockPos(27, 100, 0);
                player.setStackInHand(Hand.MAIN_HAND, dropped);
                var support = destination.down();
                var placement = new ItemPlacementContext(player, Hand.MAIN_HAND, dropped,
                        new BlockHitResult(Vec3d.ofCenter(support).add(0, 0.5, 0), Direction.UP, support, false));
                require(((BlockItem) Items.FURNACE).place(placement).isAccepted(), "real BlockItem placement succeeds");
                require(world.getBlockState(destination).isOf(Blocks.FURNACE)
                        && HeatItems.elapsed(world.getBlockEntity(destination)) == 200,
                        "placed furnace retains its clock instead of becoming full");
                // A completely depleted campfire is placed extinguished, with no free burning tick.
                var empty = new ItemStack(Items.CAMPFIRE);
                empty.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.campfireExposureSeconds);
                player.setStackInHand(Hand.MAIN_HAND, empty);
                var emptySupport = new BlockPos(29, 99, 0);
                require(((BlockItem) Items.CAMPFIRE).place(new ItemPlacementContext(player, Hand.MAIN_HAND, empty,
                        new BlockHitResult(Vec3d.ofCenter(emptySupport).add(0, 0.5, 0), Direction.UP, emptySupport, false))).isAccepted(),
                        "depleted campfire can be placed");
                require(!world.getBlockState(emptySupport.up()).get(Properties.LIT), "depleted campfire is immediately unlit");
                player.getInventory().clear();
                var recovering = new ItemStack(Items.CAMPFIRE);
                recovering.set(HeatItems.EXPOSURE, 100);
                player.getInventory().setStack(0, recovering);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                int elapsed = HeatItems.elapsed(player.getInventory().getStack(0));
                require(elapsed == 92, "two real seconds of backpack recovery restore eight seconds: " + elapsed);
                for (var item : new net.minecraft.item.Item[]{Items.CAMPFIRE, Items.SOUL_CAMPFIRE,
                        Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER}) {
                    var stack = new ItemStack(item);
                    int limit = HeatItems.limit(item, ExtremeWinter.CONFIG);
                    require(!stack.isItemBarVisible(), "unused heat source has no persistent bar");
                    stack.set(HeatItems.EXPOSURE, limit);
                    require(stack.isItemBarVisible() && stack.getItemBarStep() == 0, "depleted source displays empty bar");
                    for (int second = 0; second < 30; second++) HeatItems.recover(stack, ExtremeWinter.CONFIG);
                    require(HeatItems.elapsed(stack) == 0 && !stack.isItemBarVisible() && stack.getItemBarStep() == 13,
                            "fully recovered heat source hides its bar");
                }
                player.getInventory().clear();
                for (int slot = 0; slot < 4; slot++) {
                    var stack = new ItemStack(Items.CAMPFIRE);
                    int fixtureElapsed = new int[]{0, 60, 108, 120}[slot];
                    stack.set(HeatItems.EXPOSURE, fixtureElapsed);
                    player.getInventory().setStack(slot, stack);
                }
                var items = new net.minecraft.item.Item[]{Items.SOUL_CAMPFIRE, Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER};
                for (int slot = 0; slot < items.length; slot++) {
                    var stack = new ItemStack(items[slot]);
                    stack.set(HeatItems.EXPOSURE, HeatItems.limit(items[slot], ExtremeWinter.CONFIG) / 2);
                    player.getInventory().setStack(slot + 4, stack);
                }
                var sword = new ItemStack(Items.STONE_SWORD);
                sword.setDamage(80);
                player.getInventory().setStack(8, sword);
                require(sword.isItemBarVisible(), "ordinary weapon durability is retained");
                player.getInventory().markDirty();
            });
            context.waitTicks(2);
            context.runOnClient(client -> {
                require(client.player.getInventory().getStack(1).getItemBarStep() >= 6
                        && client.player.getInventory().getStack(1).getItemBarStep() <= 7, "item component and bar sync to client");
                client.setScreen(new InventoryScreen(client.player));
            });
            context.takeScreenshot("heat-charge-inventory-" + System.getProperty("winter.test.profile", "A"));
            context.setScreen(() -> null);
            game.getServer().runOnServer(server -> {
                var stored = new ItemStack(Items.CAMPFIRE);
                stored.set(HeatItems.EXPOSURE, 80);
                server.getPlayerManager().getPlayerList().getFirst().getInventory().setStack(9, stored);
            });
        }
        try (var game = first.open()) {
            game.getServer().runOnServer(server -> {
                var stored = server.getPlayerManager().getPlayerList().getFirst().getInventory().getStack(9);
                require(stored.isOf(Items.CAMPFIRE) && HeatItems.elapsed(stored) > 0 && HeatItems.elapsed(stored) <= 80,
                        "partially charged inventory item survives save/reopen without becoming full");
            });
        }
        ExtremeWinter.LOGGER.info("TEST vanilla spawn, two new saves, one starter campfire, portable clocks and inventory bars PASSED");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
