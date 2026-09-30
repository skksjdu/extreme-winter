package dev.extremewinter.test;

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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.GameRules;

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
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                player.getInventory().clear();
                player.changeGameMode(GameMode.CREATIVE);
                player.teleport(world, 24.5, 100, 0.5, Set.of(), 0, 0, true);
                world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, server);
                world.setTimeOfDay(6000);
                world.setWeather(6000, 0, false, false);
                for (int x = 14; x <= 32; x++) for (int z = -4; z <= 5; z++) {
                    world.setBlockState(new BlockPos(x, 99, z), Blocks.STONE.getDefaultState());
                }
                world.setBlockState(CAMP, Blocks.CAMPFIRE.getDefaultState());
                HeatItems.writeExposure(world.getBlockEntity(CAMP), 20);
                world.setBlockState(TORCH, Blocks.TORCH.getDefaultState());
                world.setBlockState(SOUL.west(), Blocks.STONE.getDefaultState());
                world.setBlockState(SOUL, Blocks.SOUL_WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST));
                var torches = new TorchWeathering(ExtremeWinter.CONFIG);
                var torchChunk = world.getChunkManager().getWorldChunk(TORCH.getX() >> 4, TORCH.getZ() >> 4);
                require(torchChunk != null, "torch fixture chunk is already loaded");
                torches.discoverChunk(world, torchChunk);
                require(TorchCoolingState.get(world).contains(TORCH) && TorchCoolingState.get(world).contains(SOUL),
                        "palette discovery finds floor and wall torches without block entities");
                TorchCoolingState.get(world).track(TORCH, ExtremeWinter.CONFIG.torchExposureSeconds - 2);
                TorchCoolingState.get(world).track(SOUL, ExtremeWinter.CONFIG.torchExposureSeconds - 2);
                var mined = Block.getDroppedStacks(world.getBlockState(TORCH), world, TORCH, null, player, ItemStack.EMPTY);
                require(mined.size() == 1 && HeatItems.elapsed(mined.getFirst()) == ExtremeWinter.CONFIG.torchExposureSeconds - 2,
                        "normal torch mining preserves its clock without a block entity");
                var cold = new ItemStack(Items.TORCH);
                cold.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                player.setStackInHand(Hand.MAIN_HAND, cold);
                require(!place(player, cold, new BlockPos(26, 100, 0)) && cold.getCount() == 1,
                        "cold torch placement is denied without consuming an item");
                player.getInventory().clear();
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == 22,
                        "creative player and frozen daylight do not pause an exposed campfire");
                require(world.getBlockState(TORCH).isAir() && world.getBlockState(SOUL).isAir(),
                        "cold floor and wall torches become air");
                for (var pos : new BlockPos[]{TORCH, SOUL}) {
                    var drops = world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(2),
                            item -> HeatItems.isTorch(item.getStack()));
                    require(drops.size() == 1 && drops.getFirst().getStack().getCount() == 1
                            && HeatItems.elapsed(drops.getFirst().getStack()) > 0,
                            "each cold torch creates exactly one cooling item entity");
                }
                clock[0] = HeatItems.elapsed(world.getBlockEntity(CAMP));
                world.setBlockState(CAMP.up(3), Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true));
            });
            context.waitTicks(120);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == clock[0], "tree canopy deliberately pauses campfire wear");
                var drops = world.getEntitiesByClass(ItemEntity.class, new Box(14, 99, -2, 20, 104, 5),
                        item -> HeatItems.isTorch(item.getStack()));
                require(drops.size() == 2 && drops.stream().allMatch(item -> HeatItems.elapsed(item.getStack()) == 0),
                        "fallen torches recover on the ground in about five real game seconds");
                var ready = drops.stream().filter(item -> item.getStack().isOf(Items.TORCH)).findFirst().orElseThrow().getStack().copy();
                player.setStackInHand(Hand.MAIN_HAND, ready);
                require(place(player, ready, new BlockPos(26, 100, 0)), "recovered torch can be placed again");
                require(TorchCoolingState.get(world).elapsed(new BlockPos(26, 100, 0)) == 0,
                        "real torch placement registers a new full clock");
                world.setBlockState(new BlockPos(26, 100, 0), Blocks.AIR.getDefaultState());
                world.setBlockState(CAMP.up(3), Blocks.AIR.getDefaultState());
                player.changeGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                var recovering = new ItemStack(Items.TORCH);
                recovering.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                player.getInventory().setStack(0, recovering);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == clock[0] + 2,
                        "removing cover resumes the same clock in survival mode");
                require(HeatItems.elapsed(player.getInventory().getStack(0)) == 27,
                        "inventory torch cooldown restores nine seconds of durability per second");
                world.setBlockState(CAMP, Blocks.AIR.getDefaultState());
                player.teleport(world, 22.5, 100, 0.5, Set.of(), 0, 0, true);
                var first = new BlockPos(24, 100, -1);
                var second = new BlockPos(24, 100, 1);
                world.setBlockState(first, Blocks.CAMPFIRE.getDefaultState());
                var heat = new HeatSources(ExtremeWinter.CONFIG);
                double single = heat.strength(player);
                world.setBlockState(second, Blocks.CAMPFIRE.getDefaultState());
                double combined = heat.strength(player);
                require(single > 0 && combined > single && combined <= ExtremeWinter.CONFIG.maxHeatStrength,
                        "two visible heat sources provide greater warmth than one");
                for (int z = -2; z <= 2; z++) for (int y = 100; y <= 103; y++) {
                    world.setBlockState(new BlockPos(23, y, z), Blocks.STONE.getDefaultState());
                }
                require(heat.strength(player) == 0, "stacking does not bypass walls");
                for (int z = -2; z <= 2; z++) for (int y = 100; y <= 103; y++) {
                    world.setBlockState(new BlockPos(23, y, z), Blocks.AIR.getDefaultState());
                }
                world.setBlockState(first, Blocks.AIR.getDefaultState());
                world.setBlockState(second, Blocks.AIR.getDefaultState());
                world.setBlockState(first, Blocks.TORCH.getDefaultState());
                double weak = heat.strength(player);
                require(weak > 0 && weak < single, "placed torch provides weaker warmth than a campfire");
                world.setBlockState(first, Blocks.AIR.getDefaultState());
                world.setTimeOfDay(2 * 24000 + 6000);
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var player = server.getPlayerManager().getPlayerList().getFirst();
                earlyLoss[0] = 80 - TemperatureData.get(player);
                require(earlyLoss[0] > 0, "early calendar stage loses heat outdoors");
                server.getOverworld().setTimeOfDay(3 * 24000 + 6000);
                TemperatureData.set(player, 80);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                var player = server.getPlayerManager().getPlayerList().getFirst();
                double lateLoss = 80 - TemperatureData.get(player);
                require(Math.abs(lateLoss / earlyLoss[0] - 1.1) < 0.001,
                        "third completed game day increases real player heat loss by ten percent");
                world.setBlockState(TORCH, Blocks.TORCH.getDefaultState());
                world.setBlockState(TORCH.up(3), Blocks.GLASS.getDefaultState());
                TorchCoolingState.get(world).track(TORCH, 12);
                world.setBlockState(CAMP, Blocks.CAMPFIRE.getDefaultState());
                world.setBlockState(CAMP.up(3), Blocks.GLASS.getDefaultState());
                HeatItems.writeExposure(world.getBlockEntity(CAMP), 30);
            });
        }
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var world = server.getOverworld();
                require(TorchCoolingState.get(world).elapsed(TORCH) == 12, "placed torch clock survives save/reopen");
                require(HeatItems.elapsed(world.getBlockEntity(CAMP)) == 30, "covered campfire clock survives save/reopen");
                require(WinterProgression.stage(world.getTimeOfDay(), ExtremeWinter.CONFIG) == 1,
                        "winter stage follows saved calendar time");
                world.setBlockState(CAMP.up(3), Blocks.AIR.getDefaultState());
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> require(
                    HeatItems.elapsed(server.getOverworld().getBlockEntity(CAMP)) == 32,
                    "reopened campfire resumes consumption when uncovered"));
        }
        ExtremeWinter.LOGGER.info("TEST torch fall/cooldown, stacked warmth, winter stages and creative/survival campfire clocks PASSED");
    }

    private static boolean place(net.minecraft.server.network.ServerPlayerEntity player, ItemStack stack, BlockPos pos) {
        var support = pos.down();
        return ((BlockItem) stack.getItem()).place(new ItemPlacementContext(player, Hand.MAIN_HAND, stack,
                new BlockHitResult(Vec3d.ofCenter(support).add(0, 0.5, 0), Direction.UP, support, false))).isAccepted();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
