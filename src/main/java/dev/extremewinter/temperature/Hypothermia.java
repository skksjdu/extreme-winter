package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Hypothermia {
    private Hypothermia() { }

    public static void apply(ServerPlayerEntity player, double temperature, int serverTicks, WinterConfig config) {
        if (player.isCreative() || player.isSpectator()) return;
        int stage = TemperatureModel.stage(temperature, config);
        // Short refreshes expire naturally. Never remove effects belonging to potions or other mods.
        if (stage >= 2) player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, false, false, true));
        if (stage >= 3) player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 0, false, false, true));
        float damage = TemperatureModel.freezingDamage(temperature, config);
        if (damage > 0 && Math.floorMod(serverTicks + player.getId(), 20 * config.damageIntervalSeconds) == 0) {
            player.damage(player.getWorld(), player.getDamageSources().freeze(), damage);
        }
    }
}
