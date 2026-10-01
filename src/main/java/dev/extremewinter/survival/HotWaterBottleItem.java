package dev.extremewinter.survival;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import dev.extremewinter.temperature.WinterWorldState;

public final class HotWaterBottleItem extends Item {
    public HotWaterBottleItem(Properties properties) { super(properties); }
    private static InteractionResult fill(Level level, Player player, InteractionHand hand, BlockPos pos) {
        var stack = player.getItemInHand(hand);
        if (stack.getOrDefault(WinterGear.FILLED, false) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, stack)) return InteractionResult.PASS;
        var state = level.getBlockState(pos);
        boolean cauldron = state.is(Blocks.WATER_CAULDRON);
        if (!cauldron && !(state.is(Blocks.WATER) && level.getFluidState(pos).isSource())) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (cauldron) {
                int amount = state.getValue(LayeredCauldronBlock.LEVEL);
                level.setBlock(pos, amount == 1 ? Blocks.CAULDRON.defaultBlockState()
                        : state.setValue(LayeredCauldronBlock.LEVEL, amount - 1), Block.UPDATE_ALL);
            }
            stack.set(WinterGear.FILLED, true);
            stack.set(WinterGear.CHARGE, 0);
            stack.remove(WinterGear.EXPIRY);
            player.getInventory().setChanged();
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : fill(context.getLevel(), context.getPlayer(), context.getHand(), context.getClickedPos());
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        return hit.getType() == HitResult.Type.BLOCK ? fill(level, player, hand, hit.getBlockPos()) : InteractionResult.PASS;
    }
    public static boolean canCharge(ServerPlayer player) {
        return dev.extremewinter.temperature.HeatSources.canCharge(player);
    }
    @Override public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        long now = WinterWorldState.get(world).elapsedTicks();
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!held || !stack.getOrDefault(WinterGear.FILLED, false) || !canCharge(player)) {
            if (stack.getOrDefault(WinterGear.CHARGE, 0) != 0) stack.set(WinterGear.CHARGE, 0);
            stack.remove(WinterGear.LAST_CHARGE);
            return;
        }
        int progress = stack.getOrDefault(WinterGear.LAST_CHARGE, -2L) == now - 1 ? stack.getOrDefault(WinterGear.CHARGE, 0) : 0;
        // Zero elapsed ticks means the world clock is paused; repeated inventory calls cannot fill it.
        if (stack.getOrDefault(WinterGear.LAST_CHARGE, -2L) == now) return;
        stack.set(WinterGear.LAST_CHARGE, now);
        if (++progress >= 200) {
            stack.set(WinterGear.EXPIRY, now + WinterGear.BOTTLE_DURATION);
            WinterTasks.award(player, "bottle");
            progress = 0;
        }
        stack.set(WinterGear.CHARGE, progress);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return stack.getOrDefault(WinterGear.FILLED, false); }
    @Override public int getBarWidth(ItemStack stack) {
        return Math.round(13f * WinterGear.remaining(stack, WinterGear.clientClock()) / WinterGear.BOTTLE_DURATION);
    }
    @Override public int getBarColor(ItemStack stack) { return 0xe59c52; }
}
