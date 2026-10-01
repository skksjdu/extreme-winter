package dev.extremewinter.survival;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.environment.WinterWeatherModel.Weather;
import dev.extremewinter.temperature.WinterProgression;
import dev.extremewinter.temperature.WinterWorldState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native advancements driven by successful actions and actual spawned harvest produce. */
public final class WinterTasks {
    public static final AttachmentType<Integer> HARVESTS = AttachmentRegistry.create(WinterGear.id("greenhouse_harvest_count"),
            b -> b.initializer(() -> 0).persistent(Codec.intRange(0, Integer.MAX_VALUE)).copyOnDeath());
    public static final AttachmentType<Boolean> STOVE_USED = flag("owned_stove_has_burned");
    public static final AttachmentType<Boolean> STORM_SEEN = flag("blizzard_experienced");
    public static final AttachmentType<Integer> STORM_TICKS = AttachmentRegistry.create(WinterGear.id("blizzard_experience_ticks"),
            b -> b.initializer(() -> 0).persistent(Codec.intRange(0, 72000)).copyOnDeath());
    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();
    private static final class Harvest {
        final ServerPlayer player;
        final BlockPos pos;
        final Item produce;
        int count;
        Harvest(ServerPlayer player, BlockPos pos, Item produce) { this.player = player; this.pos = pos.immutable(); this.produce = produce; }
    }
    private WinterTasks() { }
    private static AttachmentType<Boolean> flag(String name) {
        return AttachmentRegistry.create(WinterGear.id(name), b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    }
    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            var harvest = HARVEST.get();
            if (harvest != null && world == harvest.player.level() && entity instanceof ItemEntity item
                    && item.getItem().is(harvest.produce) && item.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(harvest.pos)) < 4) {
                harvest.count = Math.clamp((long) harvest.count + item.getItem().getCount(), 0, Integer.MAX_VALUE);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(WinterTasks::tick);
    }
    public static void award(ServerPlayer player, String name) {
        if (player.isCreative() || player.isSpectator()) return;
        var advancement = player.level().getServer().getAdvancements().get(WinterGear.id("survival/" + name));
        if (advancement != null) player.getAdvancements().award(advancement, "performed");
    }
    public static void warmed(ServerPlayer player, double before, double after, double heat) {
        if (player.level().dimension().equals(Level.OVERWORLD) && after > before && heat > 0) award(player, "warming");
    }
    public static void stoveBurned(ServerPlayer owner) { owner.setAttached(STOVE_USED, true); checkHome(owner); }
    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (var p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator() || !p.level().dimension().equals(Level.OVERWORLD)) continue;
            int ticks = p.getAttachedOrCreate(STORM_TICKS);
            if (WinterWeatherController.current(p.level()) == Weather.BLIZZARD) {
                p.setAttached(STORM_TICKS, Math.min(72000, ticks + 20));
            } else if (ticks > 0) {
                // A real interval in the storm followed by its end; a warning/UI never grants it.
                if (ticks >= 200) { p.setAttached(STORM_SEEN, true); award(p, "storm"); }
                p.setAttached(STORM_TICKS, 0);
            }
            checkHome(p);
        }
    }
    private static void checkHome(ServerPlayer p) {
        if (WinterProgression.stage(WinterWorldState.get(p.level()).elapsedTicks(), ExtremeWinter.CONFIG) == 4
                && p.getAttachedOrCreate(STOVE_USED) && p.getAttachedOrCreate(STORM_SEEN) && p.getAttachedOrCreate(HARVESTS) >= 32) award(p, "home");
    }
    public static void beginHarvest(ServerPlayer p, BlockPos pos, BlockState state) {
        HARVEST.remove();
        if (p.isCreative() || p.isSpectator() || !WinterFarming.greenhouse(p.level(), pos)) return;
        Item produce = produce(p, pos, state);
        if (produce != null) HARVEST.set(new Harvest(p, pos, produce));
    }
    public static void finishHarvest(boolean success) {
        var harvest = HARVEST.get(); HARVEST.remove();
        if (!success || harvest == null || harvest.count == 0) return;
        var p = harvest.player;
        p.setAttached(HARVESTS, Math.clamp((long) p.getAttachedOrCreate(HARVESTS) + harvest.count, 0, Integer.MAX_VALUE));
        award(p, "harvest"); checkHome(p);
    }
    private static Item produce(ServerPlayer p, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop && state.is(WinterFarming.CROPS) && crop.isMaxAge(state)) {
            if (state.is(Blocks.WHEAT)) return Items.WHEAT;
            if (state.is(Blocks.CARROTS)) return Items.CARROT;
            if (state.is(Blocks.POTATOES)) return Items.POTATO;
            if (state.is(Blocks.BEETROOTS)) return Items.BEETROOT;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2) return Items.SWEET_BERRIES;
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            var attached = state.is(Blocks.PUMPKIN) ? Blocks.ATTACHED_PUMPKIN_STEM : Blocks.ATTACHED_MELON_STEM;
            for (var direction : Direction.Plane.HORIZONTAL) {
                var neighbor = pos.relative(direction);
                if (p.level().getChunkSource().getChunkNow(neighbor.getX() >> 4, neighbor.getZ() >> 4) == null) continue;
                var stem = p.level().getBlockState(neighbor);
                if (stem.is(attached) && stem.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction.getOpposite()) {
                    return state.is(Blocks.PUMPKIN) ? Items.PUMPKIN : Items.MELON_SLICE;
                }
            }
        }
        return null;
    }
}
