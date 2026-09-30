package dev.extremewinter.temperature;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A persistent, network-synced clock shared by placed heat sources and their item stacks. */
public final class HeatItems {
    public static final ComponentType<Integer> EXPOSURE = Registry.register(Registries.DATA_COMPONENT_TYPE,
            Identifier.of(ExtremeWinter.ID, "heat_exposure_seconds"), ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 604800)).packetCodec(PacketCodecs.VAR_INT).build());

    private HeatItems() { }
    public static void initialize() { }

    public static int limit(Item item, WinterConfig config) {
        if (item == Items.CAMPFIRE) return config.campfireExposureSeconds;
        if (item == Items.SOUL_CAMPFIRE) return config.soulCampfireExposureSeconds;
        if (item == Items.FURNACE) return config.furnaceExposureSeconds;
        if (item == Items.BLAST_FURNACE) return config.blastFurnaceExposureSeconds;
        if (item == Items.SMOKER) return config.smokerExposureSeconds;
        return 0;
    }

    public static boolean supported(ItemStack stack) { return limit(stack.getItem(), ExtremeWinter.CONFIG) > 0; }
    public static int elapsed(ItemStack stack) { return stack.getOrDefault(EXPOSURE, 0); }
    public static int elapsed(BlockEntity entity) {
        return entity.getComponents().getOrDefault(EXPOSURE, entity.getAttachedOrCreate(HeatWeathering.EXPOSURE));
    }

    public static void writeExposure(BlockEntity entity, int seconds) {
        entity.setAttached(HeatWeathering.EXPOSURE, seconds);
        entity.setComponents(ComponentMap.builder().addAll(entity.getComponents()).add(EXPOSURE, seconds).build());
        entity.markDirty();
    }

    public static void copyToDrop(BlockEntity entity, ItemStack stack) {
        if (entity != null && supported(stack)) {
            int seconds = Math.min(limit(stack.getItem(), ExtremeWinter.CONFIG), elapsed(entity));
            if (seconds > 0) stack.set(EXPOSURE, seconds);
        }
    }

    public static void onPlaced(World world, BlockPos pos, ItemStack stack) {
        if (!(world instanceof ServerWorld) || !supported(stack)) return;
        var entity = world.getBlockEntity(pos);
        if (entity == null) return;
        int limit = limit(stack.getItem(), ExtremeWinter.CONFIG);
        int seconds = Math.min(limit, elapsed(stack));
        writeExposure(entity, seconds);
        entity.setAttached(HeatWeathering.BLOCKED, seconds >= limit && entity instanceof AbstractFurnaceBlockEntity);
        var state = world.getBlockState(pos);
        entity.setAttached(HeatWeathering.WAS_LIT, seconds < limit && state.get(Properties.LIT));
        if (seconds >= limit && state.get(Properties.LIT)) {
            world.setBlockState(pos, state.with(Properties.LIT, false), Block.NOTIFY_ALL);
        }
    }

    public static void recover(ItemStack stack, WinterConfig config) {
        int limit = limit(stack.getItem(), config);
        int seconds = elapsed(stack);
        if (limit == 0 || seconds == 0) return;
        int recovered = Math.max(1, (int) Math.ceil(limit / (double) config.heatRecoverySeconds));
        int remaining = Math.max(0, seconds - recovered);
        if (remaining == 0) stack.remove(EXPOSURE);
        else stack.set(EXPOSURE, remaining);
    }

    public static void tickInventories(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        for (var player : server.getPlayerManager().getPlayerList()) {
            var inventory = player.getInventory();
            boolean changed = false;
            for (int slot = 0; slot < inventory.size(); slot++) {
                var stack = inventory.getStack(slot);
                int before = elapsed(stack);
                recover(stack, ExtremeWinter.CONFIG);
                changed |= before != elapsed(stack);
            }
            if (changed) inventory.markDirty();
        }
    }

    public static float fraction(ItemStack stack) {
        int limit = limit(stack.getItem(), ExtremeWinter.CONFIG);
        return limit == 0 ? 1 : MathHelper.clamp(1 - elapsed(stack) / (float) limit, 0, 1);
    }
    public static int barStep(ItemStack stack) { return Math.round(13 * fraction(stack)); }
    public static int barColor(ItemStack stack) { return MathHelper.hsvToRgb(fraction(stack) / 3, 1, 1); }

    public static ActionResult pickUpCampfire(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || !player.isSneaking() || !player.getStackInHand(hand).isEmpty()
                || player.isSpectator() || !player.canModifyBlocks()) return ActionResult.PASS;
        var state = world.getBlockState(hit.getBlockPos());
        if (!state.isOf(Blocks.CAMPFIRE) && !state.isOf(Blocks.SOUL_CAMPFIRE)) return ActionResult.PASS;
        if (world instanceof ServerWorld) {
            if (!world.canEntityModifyAt(player, hit.getBlockPos())) return ActionResult.PASS;
            var entity = world.getBlockEntity(hit.getBlockPos());
            var stack = new ItemStack(state.getBlock().asItem());
            copyToDrop(entity, stack);
            // Vanilla removal drops any cooking ingredients; we return only the campfire itself.
            if (!world.removeBlock(hit.getBlockPos(), false)) return ActionResult.PASS;
            if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
            player.getInventory().markDirty();
        }
        return ActionResult.SUCCESS;
    }
}
