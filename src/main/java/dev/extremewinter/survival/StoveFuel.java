package dev.extremewinter.survival;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StoveFuel {
    private StoveFuel() { }
    public static int ticks(ItemStack stack) {
        if (stack.is(Items.COAL_BLOCK)) return 72 * 60 * 20;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 8 * 60 * 20;
        if (stack.is(ItemTags.LOGS_THAT_BURN)) return 80 * 20;
        if (stack.is(ItemTags.PLANKS)) return 20 * 20;
        if (stack.is(Items.STICK)) return 10 * 20;
        return 0;
    }
}
