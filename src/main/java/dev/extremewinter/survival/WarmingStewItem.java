package dev.extremewinter.survival;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WarmingStewItem extends Item {
    public WarmingStewItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        var remaining = super.finishUsingItem(stack, level, entity);
        if (entity instanceof ServerPlayer player) WinterGear.eatStew(player);
        return remaining;
    }
}
