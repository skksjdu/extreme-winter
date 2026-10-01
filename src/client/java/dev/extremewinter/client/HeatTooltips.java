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
        boolean simple = stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.FURNACE) || stack.is(Items.BLAST_FURNACE) || stack.is(Items.SMOKER);
        if (!HeatItems.supported(stack) && !lava && !simple) return;
        add(lines, "heat", ChatFormatting.GOLD, config.heatSourceRadius);
        add(lines, "shelter", ChatFormatting.DARK_GRAY);
        if (lava) {
            add(lines, config.outdoorHeatExtinguishing && config.lavaCooling ? "lava" : "lava_stable", ChatFormatting.GRAY);
            if (config.outdoorHeatExtinguishing && config.lavaCooling) add(lines, "lava_cooling", ChatFormatting.GRAY, config.lavaExposureSeconds);
            add(lines, "lava_pickup", ChatFormatting.GRAY);
        } else {
            boolean torch = HeatItems.isTorch(stack);
            boolean campfire = stack.is(Items.CAMPFIRE) || stack.is(Items.SOUL_CAMPFIRE);
            int limit = HeatItems.limit(stack.getItem(), config);
            add(lines, torch ? (limit > 0 ? "torch" : "torch_stable") : campfire ? "campfire" : (limit > 0 ? "furnace" : "furnace_stable"), ChatFormatting.GRAY);
            if (limit > 0) {
            int remaining = Math.max(0, limit - HeatItems.elapsed(stack));
            ChatFormatting color = remaining > limit * 2 / 3 ? ChatFormatting.GREEN
                    : remaining > limit / 3 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            add(lines, "durability", color, Math.round(HeatItems.fraction(stack) * 100), remaining, limit);
            add(lines, torch ? "torch_recovery" : "recovery", ChatFormatting.GRAY,
                    torch ? config.torchRecoverySeconds : config.heatRecoverySeconds);
            if (torch && HeatItems.elapsed(stack) > 0) {
                add(lines, "torch_cooldown", ChatFormatting.YELLOW, HeatItems.cooldownSeconds(stack, config));
            }
            }
            add(lines, torch ? (limit > 0 ? "torch_pickup" : "torch_pickup_stable") : campfire ? "campfire_pickup" : limit > 0 ? "furnace_pickup" : "furnace_pickup_stable", ChatFormatting.GRAY);
            if (campfire) add(lines, "cooking_drop", ChatFormatting.DARK_GRAY);
        }
        if (!config.outdoorHeatExtinguishing) add(lines, "weather_disabled", ChatFormatting.DARK_GRAY);
    }

    private static void add(List<Component> lines, String key, ChatFormatting color, Object... values) {
        lines.add(Component.translatable("tooltip.extreme_winter." + key, values).withStyle(color));
    }
}
