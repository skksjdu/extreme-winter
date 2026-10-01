package dev.extremewinter.survival;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.Exposure;
import dev.extremewinter.temperature.HeatSources;
import dev.extremewinter.temperature.TemperatureData;
import dev.extremewinter.temperature.WinterProgression;
import dev.extremewinter.temperature.WinterWorldState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class WarmthMeterItem extends Item {
    public WarmthMeterItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) {
            var cfg = ExtremeWinter.CONFIG;
            int stage = WinterProgression.stage(WinterWorldState.get(p.level()).elapsedTicks(), cfg);
            boolean sheltered = !Exposure.outdoors(p.level(), p.blockPosition().above());
            double heat = new HeatSources(cfg).strength(p);
            p.sendSystemMessage(Component.translatable("message.extreme_winter.meter",
                    String.format(java.util.Locale.ROOT, "%.1f", TemperatureData.get(p)),
                    Component.translatable("winter.extreme_winter." + stage),
                    Component.translatable(sheltered ? "shelter.extreme_winter.covered" : "shelter.extreme_winter.exposed"),
                    String.format(java.util.Locale.ROOT, "%.2f", heat)));
        }
        return InteractionResult.SUCCESS;
    }
}
