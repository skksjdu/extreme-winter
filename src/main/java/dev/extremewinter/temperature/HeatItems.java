package dev.extremewinter.temperature;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.WeakHashMap;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.config.WinterConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** A persistent, network-synced clock shared by placed heat sources and their item stacks. */
public final class HeatItems {
    private static final Map<ItemEntity, Integer> TORCH_DROPS = new WeakHashMap<>();
    public static final DataComponentType<Integer> EXPOSURE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "heat_exposure_seconds"), DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 604800)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private HeatItems() { }
    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && isTorch(item.getItem())) TORCH_DROPS.put(item, item.tickCount);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> TORCH_DROPS.remove(entity));
        ServerTickEvents.END_LEVEL_TICK.register(HeatItems::recoverDroppedTorches);
    }

    public static boolean isTorch(ItemStack stack) { return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH); }
    public static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    public static int limit(Item item, WinterConfig config) {
        if (!config.outdoorHeatExtinguishing) return 0;
        if ((item == Items.FURNACE || item == Items.BLAST_FURNACE || item == Items.SMOKER) && !config.furnaceWeathering) return 0;
        if ((item == Items.TORCH || item == Items.SOUL_TORCH) && !config.torchWeathering) return 0;
        if (item == Items.CAMPFIRE) return config.campfireExposureSeconds;
        if (item == Items.SOUL_CAMPFIRE) return config.soulCampfireExposureSeconds;
        if (item == Items.FURNACE) return config.furnaceExposureSeconds;
        if (item == Items.BLAST_FURNACE) return config.blastFurnaceExposureSeconds;
        if (item == Items.SMOKER) return config.smokerExposureSeconds;
        if (item == Items.TORCH || item == Items.SOUL_TORCH) return config.torchExposureSeconds;
        return 0;
    }

    public static boolean supported(ItemStack stack) { return limit(stack.getItem(), ExtremeWinter.CONFIG) > 0; }
    public static int elapsed(ItemStack stack) { return stack.getOrDefault(EXPOSURE, 0); }
    public static int elapsed(BlockEntity entity) {
        return entity.components().getOrDefault(EXPOSURE, entity.getAttachedOrCreate(HeatWeathering.EXPOSURE));
    }

    public static void writeExposure(BlockEntity entity, int seconds) {
        entity.setAttached(HeatWeathering.EXPOSURE, seconds);
        entity.setComponents(DataComponentMap.builder().addAll(entity.components()).set(EXPOSURE, seconds).build());
        entity.setChanged();
    }

    public static void copyToDrop(BlockEntity entity, ItemStack stack) {
        if (entity != null && supported(stack)) {
            int seconds = Math.min(limit(stack.getItem(), ExtremeWinter.CONFIG), elapsed(entity));
            if (seconds > 0) stack.set(EXPOSURE, seconds);
        }
    }

    public static void copyToDrop(ServerLevel world, BlockPos pos, BlockEntity entity, ItemStack stack) {
        if (isTorch(stack)) {
            int seconds = Math.min(limit(stack.getItem(), ExtremeWinter.CONFIG), TorchCoolingState.get(world).elapsed(pos));
            if (seconds > 0) stack.set(EXPOSURE, seconds);
        } else copyToDrop(entity, stack);
    }

    public static void onPlaced(Level world, BlockPos pos, ItemStack stack) {
        if (!(world instanceof ServerLevel)) return;
        if (!supported(stack)) {
            var disabled = world.getBlockEntity(pos);
            if (disabled instanceof AbstractFurnaceBlockEntity) {
                disabled.setAttached(HeatWeathering.BLOCKED, false);
                writeExposure(disabled, 0);
            }
            return;
        }
        if (isTorch(stack)) {
            TorchCoolingState.get((ServerLevel) world).track(pos, 0);
            return;
        }
        var entity = world.getBlockEntity(pos);
        if (entity == null) return;
        int limit = limit(stack.getItem(), ExtremeWinter.CONFIG);
        int seconds = Math.min(limit, elapsed(stack));
        writeExposure(entity, seconds);
        entity.setAttached(HeatWeathering.BLOCKED, seconds >= limit && entity instanceof AbstractFurnaceBlockEntity);
        var state = world.getBlockState(pos);
        entity.setAttached(HeatWeathering.WAS_LIT, seconds < limit && state.getValue(BlockStateProperties.LIT));
        if (seconds >= limit && state.getValue(BlockStateProperties.LIT)) {
            world.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        }
    }

    public static void recover(ItemStack stack, WinterConfig config) {
        int limit = limit(stack.getItem(), config);
        int seconds = elapsed(stack);
        if (limit == 0) { stack.remove(EXPOSURE); return; }
        if (seconds == 0) return;
        int recovered = recoveryPerSecond(stack, config);
        int remaining = Math.max(0, seconds - recovered);
        if (remaining == 0) stack.remove(EXPOSURE);
        else stack.set(EXPOSURE, remaining);
    }

    private static int recoveryPerSecond(ItemStack stack, WinterConfig config) {
        int recoverySeconds = isTorch(stack) ? config.torchRecoverySeconds : config.heatRecoverySeconds;
        return Math.max(1, (int) Math.ceil(limit(stack.getItem(), config) / (double) recoverySeconds));
    }

    public static int cooldownSeconds(ItemStack stack, WinterConfig config) {
        return isTorch(stack) ? (int) Math.ceil(elapsed(stack) / (double) recoveryPerSecond(stack, config)) : 0;
    }

    public static boolean canPlaceTorch(ItemStack stack, Player player) {
        if (!isTorch(stack) || !supported(stack) || elapsed(stack) == 0) return true;
        if (player != null) player.sendOverlayMessage(Component.translatable("message.extreme_winter.torch_cold",
                cooldownSeconds(stack, ExtremeWinter.CONFIG)).withStyle(ChatFormatting.YELLOW));
        return false;
    }

    private static void recoverDroppedTorches(ServerLevel world) {
        var entries = TORCH_DROPS.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            var item = entry.getKey();
            if (item.isRemoved() || !isTorch(item.getItem()) || elapsed(item.getItem()) == 0) {
                entries.remove();
                continue;
            }
            if (item.level() != world) continue;
            if (item.tickCount < entry.getValue()) entry.setValue(item.tickCount);
            if (item.tickCount - entry.getValue() < 20) continue;
            entry.setValue(item.tickCount);
            var stack = item.getItem().copy();
            recover(stack, ExtremeWinter.CONFIG);
            item.setItem(stack); // A new tracked value syncs the changing component to clients.
        }
    }

    public static void tickInventories(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (var player : server.getPlayerList().getPlayers()) {
            var inventory = player.getInventory();
            boolean changed = false;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                var stack = inventory.getItem(slot);
                int before = elapsed(stack);
                recover(stack, ExtremeWinter.CONFIG);
                changed |= before != elapsed(stack);
            }
            if (changed) inventory.setChanged();
        }
    }

    public static float fraction(ItemStack stack) {
        int limit = limit(stack.getItem(), ExtremeWinter.CONFIG);
        return limit == 0 ? 1 : Mth.clamp(1 - elapsed(stack) / (float) limit, 0, 1);
    }
    public static int barStep(ItemStack stack) { return Math.round(13 * fraction(stack)); }
    public static int barColor(ItemStack stack) { return Mth.hsvToRgb(fraction(stack) / 3, 1, 1); }

    public static InteractionResult pickUpCampfire(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()
                || player.isSpectator() || !player.mayBuild()) return InteractionResult.PASS;
        var state = world.getBlockState(hit.getBlockPos());
        if (!state.is(Blocks.CAMPFIRE) && !state.is(Blocks.SOUL_CAMPFIRE)) return InteractionResult.PASS;
        if (world instanceof ServerLevel) {
            if (!world.mayInteract(player, hit.getBlockPos())) return InteractionResult.PASS;
            var entity = world.getBlockEntity(hit.getBlockPos());
            var stack = new ItemStack(state.getBlock().asItem());
            copyToDrop(entity, stack);
            // Vanilla removal drops any cooking ingredients; we return only the campfire itself.
            if (!world.removeBlock(hit.getBlockPos(), false)) return InteractionResult.PASS;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.getInventory().setChanged();
        }
        return InteractionResult.SUCCESS;
    }
}
