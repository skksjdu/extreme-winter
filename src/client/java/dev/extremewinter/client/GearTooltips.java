package dev.extremewinter.client;

import java.util.List;
import dev.extremewinter.survival.WinterGear;
import dev.extremewinter.survival.WinterItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class GearTooltips {
    private GearTooltips() { }
    public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        if (WinterGear.lined(stack)) lines.add(Component.translatable("tooltip.extreme_winter.insulated").withStyle(ChatFormatting.GOLD));
        if (stack.is(WinterItems.THERMAL_LINING)) lines.add(Component.translatable("tooltip.extreme_winter.lining_recipe").withStyle(ChatFormatting.GRAY));
        if (stack.is(WinterItems.HOT_WATER_BOTTLE)) {
            lines.add(Component.translatable(stack.getOrDefault(WinterGear.FILLED, false)
                    ? "tooltip.extreme_winter.bottle_heat" : "tooltip.extreme_winter.bottle_fill").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.extreme_winter.bottle_remaining",
                    (WinterGear.remaining(stack, WinterGear.clientClock()) + 19) / 20,
                    stack.getOrDefault(WinterGear.CHARGE, 0) / 20).withStyle(ChatFormatting.GOLD));
        }
        if (stack.is(WinterItems.WARMING_STEW)) lines.add(Component.translatable("tooltip.extreme_winter.stew").withStyle(ChatFormatting.GOLD));
        if (stack.is(WinterItems.WARMTH_METER)) lines.add(Component.translatable("tooltip.extreme_winter.meter").withStyle(ChatFormatting.GRAY));
        if (stack.is(WinterItems.WEATHER_INSTRUMENT_ITEM)) lines.add(Component.translatable("tooltip.extreme_winter.instrument").withStyle(ChatFormatting.GRAY));
        if (stack.is(dev.extremewinter.survival.HeatingContent.STOVE_ITEM)) {
            lines.add(Component.translatable("tooltip.extreme_winter.stove").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.extreme_winter.stove_fuel").withStyle(ChatFormatting.GRAY));
            int remaining = stack.getOrDefault(dev.extremewinter.survival.HeatingContent.REMAINING, 0);
            if (remaining > 0) lines.add(Component.translatable("tooltip.extreme_winter.stove_saved", (remaining + 19) / 20).withStyle(ChatFormatting.GOLD));
        }
    }
}
