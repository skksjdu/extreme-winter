package dev.extremewinter.test;

import net.minecraft.world.level.gamerules.GameRules;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatItems;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.TemperatureData;
import dev.extremewinter.temperature.TorchCoolingState;
import dev.extremewinter.temperature.TorchWeathering;
import dev.extremewinter.temperature.WinterProgression;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

/** Real ticks exercise torch drops/cooldown, stacked warmth, mode changes and calendar stages. */
public final class WinterProgressionTest implements FabricClientGameTest {
    private static final BlockPos CAMP = new BlockPos(20, 100, 0);
    private static final BlockPos TORCH = new BlockPos(16, 100, 0);
    private static final BlockPos SOUL = new BlockPos(18, 101, 3);

    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        int[] clock = new int[1];
        double[] earlyLoss = new double[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(world, 24.5, 100, 0.5, Set.of(), 0, 0, true);
                world.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
                world.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
                ExposureTest.time(world, 6000);
                ExposureTest.weather(world, 6000, 0, false, false);
                for (int x = 14; x <= 32; x++) for (int z = -4; z <= 5; z++) {
                    world.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                }
                world.setBlock(CAMP, Blocks.CAMPFIRE.defaultBlockState(), 3);
                HeatItems.writeExposure(world.getBlockEntity(CAMP), 20);
                world.setBlock(TORCH, Blocks.TORCH.defaultBlockState(), 3);
                world.setBlock(SOUL.west(), Blocks.STONE.defaultBlockState(), 3);
                world.setBlock(SOUL, Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
                var torches = new TorchWeathering(ExtremeWinter.CONFIG);
                var torchChunk = world.getChunkSource().getChunkNow(TORCH.getX() >> 4, TORCH.getZ() >> 4);
                require(torchChunk != null, "torch fixture chunk is already loaded");
                torches.discoverChunk(world, torchChunk);
                require(TorchCoolingState.get(world).contains(TORCH) && TorchCoolingState.get(world).contains(SOUL),
                        "palette discovery finds floor and wall torches without block entities");
                TorchCoolingState.get(world).track(TORCH, ExtremeWinter.CONFIG.torchExposureSeconds - 2);
                TorchCoolingState.get(world).track(SOUL, ExtremeWinter.CONFIG.torchExposureSeconds - 2);
                var mined = Block.getDrops(world.getBlockState(TORCH), world, TORCH, null, player, ItemStack.EMPTY);
                require(mined.size() == 1 && HeatItems.elapsed(mined.getFirst()) == ExtremeWinter.CONFIG.torchExposureSeconds - 2,
                        "normal torch mining preserves its clock without a block entity");
                var cold = new ItemStack(Items.TORCH);
                cold.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                player.setItemInHand(InteractionHand.MAIN_HAND, cold);
                require(!place(player, cold, new BlockPos(26, 100, 0)) && cold.getCount() == 1,
                        "cold torch placement is denied without consuming an item");
                player.getInventory().clearContent();
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == 22,
                        "creative player and frozen daylight do not pause an exposed campfire");
                require(world.getBlockState(TORCH).isAir() && world.getBlockState(SOUL).isAir(),
                        "cold floor and wall torches become air");
                for (var pos : new BlockPos[]{TORCH, SOUL}) {
                    var expected = pos.equals(TORCH) ? Items.TORCH : Items.SOUL_TORCH;
                    var drops = world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                            item -> item.getItem().is(expected));
                    require(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1
                            && HeatItems.elapsed(drops.getFirst().getItem()) > 0,
                            "each cold torch creates exactly one cooling item entity at " + pos + ": "
                                    + drops.stream().map(item -> item.getItem() + " elapsed=" + HeatItems.elapsed(item.getItem())
                                            + " pos=" + item.position()).toList());
                }
                clock[0] = HeatItems.elapsed(world.getBlockEntity(CAMP));
                ExposureTest.roof(world, CAMP.above(3), Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            });
            context.waitTicks(120);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == clock[0], "tree canopy deliberately pauses campfire wear");
                var drops = world.getEntitiesOfClass(ItemEntity.class, new AABB(14, 99, -2, 20, 104, 5),
                        item -> HeatItems.isTorch(item.getItem()));
                require(drops.size() == 2 && drops.stream().allMatch(item -> HeatItems.elapsed(item.getItem()) == 0),
                        "fallen torches recover on the ground in about five real game seconds");
                var ready = drops.stream().filter(item -> item.getItem().is(Items.TORCH)).findFirst().orElseThrow().getItem().copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, ready);
                require(place(player, ready, new BlockPos(26, 100, 0)), "recovered torch can be placed again");
                require(TorchCoolingState.get(world).elapsed(new BlockPos(26, 100, 0)) == 0,
                        "real torch placement registers a new full clock");
                world.setBlock(new BlockPos(26, 100, 0), Blocks.AIR.defaultBlockState(), 3);
                ExposureTest.roof(world, CAMP.above(3), Blocks.AIR.defaultBlockState());
                player.setGameMode(GameType.SURVIVAL);
                player.getInventory().clearContent();
                var recovering = new ItemStack(Items.TORCH);
                recovering.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                player.getInventory().setItem(0, recovering);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == clock[0] + 2,
                        "removing cover resumes the same clock in survival mode");
                require(HeatItems.elapsed(player.getInventory().getItem(0)) == 27,
                        "inventory torch cooldown restores nine seconds of durability per second");
                world.setBlock(CAMP, Blocks.AIR.defaultBlockState(), 3);
                player.teleportTo(world, 22.5, 100, 0.5, Set.of(), 0, 0, true);
                var first = new BlockPos(24, 100, -1);
                var second = new BlockPos(24, 100, 1);
                world.setBlock(first, Blocks.CAMPFIRE.defaultBlockState(), 3);
                var heat = new HeatSources(ExtremeWinter.CONFIG);
                double single = heat.strength(player);
                world.setBlock(second, Blocks.CAMPFIRE.defaultBlockState(), 3);
                double combined = heat.strength(player);
                require(single > 0 && combined > single && combined <= ExtremeWinter.CONFIG.maxHeatStrength,
                        "two visible heat sources provide greater warmth than one");
                for (int z = -2; z <= 2; z++) for (int y = 100; y <= 103; y++) {
                    world.setBlock(new BlockPos(23, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
                require(heat.strength(player) == 0, "stacking does not bypass walls");
                for (int z = -2; z <= 2; z++) for (int y = 100; y <= 103; y++) {
                    world.setBlock(new BlockPos(23, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
                world.setBlock(first, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(second, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(first, Blocks.TORCH.defaultBlockState(), 3);
                double weak = heat.strength(player);
                require(weak > 0 && weak < single, "placed torch provides weaker warmth than a campfire");
                world.setBlock(first, Blocks.AIR.defaultBlockState(), 3);
                ExposureTest.time(world, 2 * 24000 + 6000);
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                earlyLoss[0] = 80 - TemperatureData.get(player);
                require(earlyLoss[0] > 0, "early calendar stage loses heat outdoors");
                ExposureTest.time(server.overworld(), 3 * 24000 + 6000);
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                double lateLoss = 80 - TemperatureData.get(player);
                require(Math.abs(lateLoss / earlyLoss[0] - 1.1) < 0.001,
                        "third completed game day increases real player heat loss by ten percent");
                world.setBlock(TORCH, Blocks.TORCH.defaultBlockState(), 3);
                ExposureTest.roof(world, TORCH.above(3), Blocks.GLASS.defaultBlockState());
                TorchCoolingState.get(world).track(TORCH, 12);
                world.setBlock(CAMP, Blocks.CAMPFIRE.defaultBlockState(), 3);
                ExposureTest.roof(world, CAMP.above(3), Blocks.GLASS.defaultBlockState());
                HeatItems.writeExposure(world.getBlockEntity(CAMP), 30);
            });
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var world = server.overworld();
                require(TorchCoolingState.get(world).elapsed(TORCH) == 12, "placed torch clock survives save/reopen");
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == 30, "covered campfire clock survives save/reopen");
                require(WinterProgression.stage(world.getOverworldClockTime(), ExtremeWinter.CONFIG) == 1,
                        "winter stage follows saved calendar time");
                ExposureTest.roof(world, CAMP.above(3), Blocks.AIR.defaultBlockState());
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> require(
                    HeatItems.elapsed(server.overworld().getBlockEntity(CAMP)) == 32,
                    "reopened campfire resumes consumption when uncovered"));
        }
        ExtremeWinter.LOGGER.info("TEST torch fall/cooldown, stacked warmth, winter stages and creative/survival campfire clocks PASSED");
    }

    private static boolean place(net.minecraft.server.level.ServerPlayer player, ItemStack stack, BlockPos pos) {
        var support = pos.below();
        return ((BlockItem) stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false))).consumesAction();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
