package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class WinterTasksTest implements FabricClientGameTest {
    private static final BlockPos CENTER = new BlockPos(8, 100, 8), CROP = CENTER.north(), STOVE = CENTER.east(2);
    private static final String[] NODES = {"roof", "warming", "lining", "bottle", "storm", "harvest", "home"};
    private static void require(boolean b, String m) { if (!b) throw new AssertionError(m); }
    private static boolean done(ServerPlayer p, String name) {
        var entry = p.level().getServer().getAdvancements().get(WinterGear.id("survival/" + name));
        require(entry != null, "native advancement exists: " + name);
        return p.getAdvancements().getOrStartProgress(entry).isDone();
    }
    private static void place(ServerPlayer p, BlockPos floor, Direction face, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        require(((BlockItem) stack.getItem()).place(new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5), face, floor, false))).consumesAction(), "actual block item placement");
    }
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save; int[] harvests = new int[1];
        String oldWeather = ExtremeWinter.CONFIG.weatherMode; ExtremeWinter.CONFIG.weatherMode = "scheduled";
        try {
            try (var game = context.worldBuilder().create()) {
                save = game.getWorldSave();
                game.getServer().runOnServer(server -> {
                    var w = server.overworld(); var p = server.getPlayerList().getPlayers().getFirst();
                    p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                    for (var name : NODES) require(!done(p, name), "joining/receiving guide grants no operation progress: " + name);
                    for (int x = 5; x <= 12; x++) for (int z = 5; z <= 11; z++) w.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                    p.teleportTo(w, 8.5, 100, 8.5, Set.of(), 0, 0, true);
                    for (int x = 6; x <= 10; x++) for (int z = 6; z <= 10; z++) if (x != 8 || z != 8) w.setBlock(new BlockPos(x, 103, z), Blocks.GLASS.defaultBlockState(), 3);
                    require(Exposure.outdoors(w, CENTER.above()), "central roof hole really leaves player exposed");
                    place(p, CENTER.above(3).north(), Direction.SOUTH, new ItemStack(Items.GLASS));
                    require(done(p, "roof"), "native final roof placement grants roof task");
                    place(p, STOVE.below(), Direction.UP, new ItemStack(HeatingContent.STOVE_ITEM));
                    w.getBlockState(STOVE).useWithoutItem(w, p, new BlockHitResult(Vec3.atCenterOf(STOVE), Direction.UP, STOVE, false));
                    require(!p.getAttachedOrCreate(WinterTasks.STOVE_USED) && !done(p, "warming"), "opening unlit stove menu grants neither burn receipt nor warmth task");
                    p.closeContainer();
                    var crafting = new CraftingMenu(47, p.getInventory(), ContainerLevelAccess.create(w, CENTER)); p.containerMenu = crafting;
                    crafting.getSlot(1).set(new ItemStack(Items.LEATHER_CHESTPLATE)); crafting.getSlot(2).set(new ItemStack(WinterItems.THERMAL_LINING));
                    crafting.slotsChanged(crafting.getSlot(1).container);
                    require(WinterGear.lined(crafting.getSlot(0).getItem()) && !done(p, "lining"), "previewing result grants no craft task");
                    crafting.clicked(0, 0, ContainerInput.PICKUP, p);
                    require(done(p, "lining"), "actually taking lined armor grants task");
                    crafting.setCarried(ItemStack.EMPTY); p.containerMenu = p.inventoryMenu;
                    ((HeatingStoveBlockEntity) w.getBlockEntity(STOVE)).setItem(0, new ItemStack(Items.COAL)); TemperatureData.set(p, 70);
                    var bag = new ItemStack(WinterItems.HOT_WATER_BOTTLE); p.setItemInHand(InteractionHand.MAIN_HAND, bag);
                    var kettle = CENTER.west(2); w.setBlock(kettle, Blocks.WATER_CAULDRON.defaultBlockState(), 3);
                    p.gameMode.useItemOn(p, w, bag, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(kettle), Direction.UP, kettle, false));
                    require(bag.getOrDefault(WinterGear.FILLED, false) && !done(p, "bottle"), "filling alone does not complete charging task");
                });
                context.waitTicks(220);
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level();
                    require(done(p, "warming") && done(p, "bottle") && p.getAttachedOrCreate(WinterTasks.STOVE_USED), "real warmth gain, continuous charge and own stove burn grant progress");
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    w.setBlock(CROP.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7), 3);
                    var mature = ((CropBlock) Blocks.POTATOES).getStateForAge(7);
                    Block.getDrops(mature, w, CROP, null, p, ItemStack.EMPTY);
                    require(p.getAttachedOrCreate(WinterTasks.HARVESTS) == 0 && !done(p, "harvest"), "loot-table calculation alone never grants harvest progress");
                    for (int harvest = 0; harvest < 32 && p.getAttachedOrCreate(WinterTasks.HARVESTS) < 32; harvest++) {
                        w.setBlock(CROP, Blocks.POTATOES.defaultBlockState(), 3);
                        for (int grow = 0; grow < 8 && !((CropBlock) Blocks.POTATOES).isMaxAge(w.getBlockState(CROP)); grow++) require(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), w, CROP), "native bone meal matures test crop");
                        require(WinterFarming.greenhouse(w, CROP), "actual crop meets greenhouse criteria");
                        int before = p.getAttachedOrCreate(WinterTasks.HARVESTS);
                        require(p.gameMode.destroyBlock(CROP), "native survival harvest succeeds");
                        require(p.getAttachedOrCreate(WinterTasks.HARVESTS) > before, "spawned native potato produce is recorded");
                    }
                    require(done(p, "harvest") && p.getAttachedOrCreate(WinterTasks.HARVESTS) >= 32, "real greenhouse produce reaches 32");
                    w.setBlock(CROP.below(), Blocks.DIRT.defaultBlockState(), 3);
                    w.setBlock(CROP, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3), 3);
                    int count = p.getAttachedOrCreate(WinterTasks.HARVESTS);
                    p.gameMode.useItemOn(p, w, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(CROP), Direction.UP, CROP, false));
                    require(w.getBlockState(CROP).getValue(SweetBerryBushBlock.AGE) == 1 && p.getAttachedOrCreate(WinterTasks.HARVESTS) > count, "native right-click berry harvest is recorded");
                    require(!done(p, "home") && !done(p, "storm"), "32 harvests without storm/long winter never complete home");
                    var state = WinterWorldState.get(w); state.setElapsedTicks(90 * 1200); new WinterWeatherController(ExtremeWinter.CONFIG).update(w);
                });
                context.waitTicks(240);
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst(); var w = p.level();
                    require(p.getAttachedOrCreate(WinterTasks.STORM_TICKS) >= 200 && !done(p, "storm"), "actual storm interval recorded; ending still required");
                    WinterWorldState.get(w).setElapsedTicks(93 * 1200); new WinterWeatherController(ExtremeWinter.CONFIG).update(w);
                });
                context.waitTicks(40);
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    require(done(p, "storm") && p.getAttachedOrCreate(WinterTasks.STORM_SEEN) && !done(p, "home"), "actual storm ending grants storm task, still before long winter");
                    WinterWorldState.get(p.level()).setElapsedTicks(270 * 1200);
                });
                context.waitTicks(40);
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    for (var name : NODES) require(done(p, name), "all seven actual operation nodes complete: " + name);
                    harvests[0] = p.getAttachedOrCreate(WinterTasks.HARVESTS);
                    p.setGameMode(GameType.SPECTATOR);
                });
            }
            try (var game = save.open()) {
                game.getServer().runOnServer(server -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    for (var name : NODES) require(done(p, name), "native advancement survives save/rejoin: " + name);
                    require(p.getAttachedOrCreate(WinterTasks.HARVESTS) == harvests[0] && p.getAttachedOrCreate(WinterTasks.STOVE_USED)
                            && p.getAttachedOrCreate(WinterTasks.STORM_SEEN), "all personal operation receipts survive rejoin");
                });
            }
        } finally { ExtremeWinter.CONFIG.weatherMode = oldWeather; }
        ExtremeWinter.LOGGER.info("TEST D seven actual operation advancements/no UI or loot preview/greenhouse harvests/storm/home/rejoin PASSED");
    }
}
