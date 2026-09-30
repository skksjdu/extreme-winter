package dev.extremewinter.client;

import java.util.List;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class HeatTooltips {
    private HeatTooltips() { }

    public static void append(ItemStack stack, Item.TooltipContext context, TooltipType type, List<Text> lines) {
        var config = ExtremeWinter.CONFIG;
        boolean lava = stack.isOf(Items.LAVA_BUCKET);
        if (!HeatItems.supported(stack) && !lava) return;
        add(lines, "heat", Formatting.GOLD, config.heatSourceRadius);
        if (lava) {
            add(lines, "lava", Formatting.GRAY);
            add(lines, "lava_cooling", Formatting.GRAY, config.lavaExposureSeconds);
            add(lines, "lava_pickup", Formatting.GRAY);
        } else {
            boolean torch = HeatItems.isTorch(stack);
            boolean campfire = stack.isOf(Items.CAMPFIRE) || stack.isOf(Items.SOUL_CAMPFIRE);
            add(lines, torch ? "torch" : campfire ? "campfire" : "furnace", Formatting.GRAY);
            int limit = HeatItems.limit(stack.getItem(), config);
            int remaining = Math.max(0, limit - HeatItems.elapsed(stack));
            Formatting color = remaining > limit * 2 / 3 ? Formatting.GREEN
                    : remaining > limit / 3 ? Formatting.YELLOW : Formatting.RED;
            add(lines, "durability", color, Math.round(HeatItems.fraction(stack) * 100), remaining, limit);
            add(lines, torch ? "torch_recovery" : "recovery", Formatting.GRAY,
                    torch ? config.torchRecoverySeconds : config.heatRecoverySeconds);
            if (torch && HeatItems.elapsed(stack) > 0) {
                add(lines, "torch_cooldown", Formatting.YELLOW, HeatItems.cooldownSeconds(stack, config));
            }
            add(lines, torch ? "torch_pickup" : campfire ? "campfire_pickup" : "furnace_pickup", Formatting.GRAY);
            if (campfire) add(lines, "cooking_drop", Formatting.DARK_GRAY);
        }
        if (!config.outdoorHeatExtinguishing) add(lines, "weather_disabled", Formatting.DARK_GRAY);
    }

    private static void add(List<Text> lines, String key, Formatting color, Object... values) {
        lines.add(Text.translatable("tooltip.extreme_winter." + key, values).formatted(color));
    }
}
