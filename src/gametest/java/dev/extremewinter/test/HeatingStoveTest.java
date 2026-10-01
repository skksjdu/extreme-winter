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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class HeatingStoveTest implements FabricClientGameTest {
    private static void require(boolean b, String m) { if (!b) throw new AssertionError(m); }
    private static final BlockPos HOME = new BlockPos(20, 100, 0);
    private static final BlockPos FAR = new BlockPos(520, 100, 8);
    private static HeatingStoveBlockEntity stove(net.minecraft.server.level.ServerLevel world, BlockPos pos) {
        return (HeatingStoveBlockEntity) world.getBlockEntity(pos);
    }
    private static void place(net.minecraft.server.level.ServerPlayer p, BlockPos pos, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var floor = pos.below();
        require(((BlockItem) stack.getItem()).place(new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0), Direction.UP, floor, false))).consumesAction(), "native stove placement succeeds");
    }
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save; int[] before = new int[1], stopped = new int[1], saved = new int[1];
        HeatingStoveBlockEntity[] unloaded = new HeatingStoveBlockEntity[1];
        try (var game = context.worldBuilder().create()) {
            save = game.getWorldSave();
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                for (int x = 14; x <= 29; x++) for (int z = -4; z <= 4; z++) {
                    w.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                    w.setBlock(new BlockPos(x, 104, z), Blocks.GLASS.defaultBlockState(), 3);
                }
                p.teleportTo(w, 18.5, 100, .5, Set.of(), 0, 0, true);
                var result = WinterGearTest.craft(p, new ItemStack(Items.FURNACE),
                        new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE));
                require(result.is(HeatingContent.STOVE_ITEM) && result.getCount() == 1, "actual nine-ingredient stove recipe");
                place(p, HOME, result);
                require(!w.getBlockState(HOME).getValue(HeatingStoveBlock.LIT), "empty stove is unlit");
                require(StoveFuel.ticks(new ItemStack(Items.OAK_LOG)) == 1600 && StoveFuel.ticks(new ItemStack(Items.OAK_PLANKS)) == 400
                        && StoveFuel.ticks(new ItemStack(Items.STICK)) == 200 && StoveFuel.ticks(new ItemStack(Items.COAL)) == 9600
                        && StoveFuel.ticks(new ItemStack(Items.CHARCOAL)) == 9600 && StoveFuel.ticks(new ItemStack(Items.COAL_BLOCK)) == 86400
                        && StoveFuel.ticks(new ItemStack(Items.DIRT)) == 0, "all configured fuel durations and invalid fuel");
                stove(w, HOME).setItem(0, new ItemStack(Items.COAL_BLOCK));
                w.getBlockState(HOME).useWithoutItem(w, p, new BlockHitResult(Vec3.atCenterOf(HOME), Direction.UP, HOME, false));
            });
            context.waitTicks(40);
            context.runOnClient(client -> {
                require(client.screen instanceof dev.extremewinter.client.HeatingStoveScreen, "actual stove screen opens");
                require(client.player.containerMenu instanceof HeatingStoveMenu m && m.remainingTicks() > 65535, "two synchronized shorts preserve a 72-minute fuel duration");
            });
            context.takeScreenshot("beginner-D-stove");
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                require(p.containerMenu instanceof HeatingStoveMenu, "native server menu exists");
                require(!p.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "slot rejects nonfuel");
                p.closeContainer();
                require(stove(w, HOME).getItem(0).isEmpty(), "one coal block consumed exactly once");
                before[0] = stove(w, HOME).remainingTicks();
                w.setBlock(HOME.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
                ((HopperBlockEntity) w.getBlockEntity(HOME.above())).setItem(0, new ItemStack(Items.COAL, 3));
                w.setBlock(HOME.below(), Blocks.HOPPER.defaultBlockState(), 3);
            });
            context.waitTicks(200);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var s = stove(w, HOME);
                require(Math.abs((before[0] - s.remainingTicks()) - 200) <= 2, "real ten seconds burns exactly 200 running ticks");
                require(s.getItem(0).is(Items.COAL) && s.getItem(0).getCount() == 3, "real upper hopper replenishes one fuel slot");
                require(((HopperBlockEntity) w.getBlockEntity(HOME.above())).isEmpty(), "hopper transferred all three fuel items");
                require(((HopperBlockEntity) w.getBlockEntity(HOME.below())).isEmpty(), "bottom hopper cannot drain unused stove fuel");
                var heat = new HeatSources(ExtremeWinter.CONFIG);
                require(Math.abs(heat.strengthAt(w, HOME.east(6)) - .3125) < 1e-9 && heat.strengthAt(w, HOME.east(7)) == 0, "range six and strength 1.25 use common falloff");
                before[0] = s.remainingTicks();
            });
            context.runOnClient(client -> client.pauseGame(false)); context.waitTicks(5);
            game.getServer().runOnServer(server -> before[0] = stove(server.overworld(), HOME).remainingTicks());
            context.waitTicks(40);
            context.runOnClient(client -> require(client.isPaused(), "actual singleplayer pause active"));
            game.getServer().runOnServer(server -> require(stove(server.overworld(), HOME).remainingTicks() == before[0], "pause consumes no stove fuel"));
            context.setScreen(() -> null);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                w.removeBlock(HOME.above(), false); w.setBlock(HOME.below(), Blocks.STONE.defaultBlockState(), 3);
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
                int clock = stove(w, HOME).remainingTicks();
                require(p.gameMode.destroyBlock(HOME), "native survival pickaxe actually removes stove");
                var drops = w.getEntitiesOfClass(ItemEntity.class, new AABB(HOME).inflate(2));
                int fuel = drops.stream().filter(e -> e.getItem().is(Items.COAL)).mapToInt(e -> e.getItem().getCount()).sum();
                var stoveDrops = drops.stream().filter(e -> e.getItem().is(HeatingContent.STOVE_ITEM)).toList();
                require(fuel == 3 && stoveDrops.size() == 1 && stoveDrops.getFirst().getItem().getCount() == 1, "breaking drops unused fuel and exactly one stove");
                var portable = stoveDrops.getFirst().getItem().copy(); stoveDrops.getFirst().discard();
                require(portable.getOrDefault(HeatingContent.REMAINING, 0) == clock, "actual drop carries exact remaining burn progress");
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, HOME.east(6)) == 0, "removal invalidates indexed heat immediately");
                place(p, HOME, portable);
                require(portable.isEmpty() && stove(w, HOME).remainingTicks() == clock, "replacing consumes drop and restores progress without a fresh fuel duration");
                stove(w, HOME).setItem(0, new ItemStack(Items.COAL, 3));
                // Test fixtures explicitly load only their remote target; production queries never load.
                w.getChunk(FAR.getX() >> 4, FAR.getZ() >> 4);
                w.setBlock(FAR.below(), Blocks.STONE.defaultBlockState(), 3);
                w.setBlock(FAR, HeatingContent.STOVE.defaultBlockState(), 3);
                stove(w, FAR).setItem(0, new ItemStack(Items.COAL_BLOCK));
                p.teleportTo(w, 518.5, 100, 8.5, Set.of(), 0, 0, true);
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                unloaded[0] = stove(w, FAR); require(unloaded[0].remainingTicks() > 86000, "remote loaded stove ticks");
                p.teleportTo(w, 1600.5, 120, .5, Set.of(), 0, 0, true); p.setGameMode(GameType.SPECTATOR);
            });
            boolean[] gone = new boolean[1];
            for (int attempt = 0; attempt < 60 && !gone[0]; attempt++) {
                context.waitTicks(20);
                game.getServer().runOnServer(server -> gone[0] = server.overworld().getChunkSource().getChunkNow(FAR.getX() >> 4, FAR.getZ() >> 4) == null);
            }
            require(gone[0], "remote stove chunk actually unloads"); stopped[0] = unloaded[0].remainingTicks();
            context.waitTicks(80);
            require(unloaded[0].remainingTicks() == stopped[0], "unloaded block entity burns no fuel while the game continues");
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, FAR.east()) == 0, "heat query cannot reload an unloaded chunk");
                require(w.getChunkSource().getChunkNow(FAR.getX() >> 4, FAR.getZ() >> 4) == null, "query leaves chunk unloaded");
                w.getChunk(FAR.getX() >> 4, FAR.getZ() >> 4); p.teleportTo(w, 518.5, 100, 8.5, Set.of(), 0, 0, true);
                saved[0] = stove(w, FAR).remainingTicks();
                require(saved[0] <= stopped[0] && saved[0] >= stopped[0] - 20, "chunk load restores progress without offline catch-up");
                ExtremeWinter.LOGGER.info("TEST D stove actual 10s decrement=200, hopper fuel=3, unloadedTicks=80, savedRemaining={}", saved[0]);
            });
        }
        context.waitTicks(40);
        try (var game = save.open()) {
            game.getServer().runOnServer(server -> {
                var w = server.overworld(); w.getChunk(FAR.getX() >> 4, FAR.getZ() >> 4);
                int value = stove(w, FAR).remainingTicks();
                require(value <= saved[0] && value >= saved[0] - 60, "real save/reopen retains progress with no clock-time catch-up");
                require(new HeatSources(ExtremeWinter.CONFIG).strengthAt(w, FAR.east()) > 0, "chunk-loaded persisted source is indexed again");
            });
        }
        ExtremeWinter.LOGGER.info("TEST D native stove crafting/menu/fuel/hoppers/drop/replace/pause/unload/rejoin PASSED");
    }
}
