package dev.extremewinter.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatItems;

public final class HeatTooltips {
    private HeatTooltips() { }

    public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag type, List<Component> lines) {
        var config = ExtremeWinter.CONFIG;
        boolean lava = stack.is(Items.LAVA_BUCKET);
        if (!HeatItems.supported(stack) && !lava) return;
        add(lines, "heat", ChatFormatting.GOLD, config.heatSourceRadius);
        add(lines, "shelter", ChatFormatting.DARK_GRAY);
        if (lava) {
            add(lines, "lava", ChatFormatting.GRAY);
            add(lines, "lava_cooling", ChatFormatting.GRAY, config.lavaExposureSeconds);
            add(lines, "lava_pickup", ChatFormatting.GRAY);
        } else {
            boolean torch = HeatItems.isTorch(stack);
            boolean campfire = stack.is(Items.CAMPFIRE) || stack.is(Items.SOUL_CAMPFIRE);
            add(lines, torch ? "torch" : campfire ? "campfire" : "furnace", ChatFormatting.GRAY);
            int limit = HeatItems.limit(stack.getItem(), config);
            int remaining = Math.max(0, limit - HeatItems.elapsed(stack));
            ChatFormatting color = remaining > limit * 2 / 3 ? ChatFormatting.GREEN
                    : remaining > limit / 3 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            add(lines, "durability", color, Math.round(HeatItems.fraction(stack) * 100), remaining, limit);
            add(lines, torch ? "torch_recovery" : "recovery", ChatFormatting.GRAY,
                    torch ? config.torchRecoverySeconds : config.heatRecoverySeconds);
            if (torch && HeatItems.elapsed(stack) > 0) {
                add(lines, "torch_cooldown", ChatFormatting.YELLOW, HeatItems.cooldownSeconds(stack, config));
            }
            add(lines, torch ? "torch_pickup" : campfire ? "campfire_pickup" : "furnace_pickup", ChatFormatting.GRAY);
            if (campfire) add(lines, "cooking_drop", ChatFormatting.DARK_GRAY);
        }
        if (!config.outdoorHeatExtinguishing) add(lines, "weather_disabled", ChatFormatting.DARK_GRAY);
    }

    private static void add(List<Component> lines, String key, ChatFormatting color, Object... values) {
        lines.add(Component.translatable("tooltip.extreme_winter." + key, values).withStyle(color));
    }
}
