package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class Hypothermia {
    private Hypothermia() { }

    public static void apply(ServerPlayer player, double temperature, int serverTicks, WinterConfig config) {
        if (player.isCreative() || player.isSpectator()) return;
        int stage = TemperatureModel.stage(temperature, config);
        // Short refreshes expire naturally. Never remove effects belonging to potions or other mods.
        if (stage >= 2) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false, true));
        if (stage >= 3) player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false, true));
        float damage = TemperatureModel.freezingDamage(temperature, config);
        if (damage > 0 && Math.floorMod(serverTicks + player.getId(), 20 * config.damageIntervalSeconds) == 0) {
            player.hurtServer(player.level(), player.damageSources().freeze(), damage);
        }
    }
}
