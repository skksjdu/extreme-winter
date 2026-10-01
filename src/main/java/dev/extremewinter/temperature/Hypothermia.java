package dev.extremewinter.temperature;
import dev.extremewinter.config.WinterConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;

public final class Hypothermia {
    private static boolean applyingColdDamage;
    private Hypothermia() { }
    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (!applyingColdDamage && taken > 0 && source.is(DamageTypes.FREEZE) && entity instanceof ServerPlayer player)
                player.setAttached(TemperatureData.FREEZE_COOLDOWN, 100);
        });
    }
    public static void apply(ServerPlayer player, double temperature, int serverTicks, WinterConfig config) {
        if (player.isCreative() || player.isSpectator()) return;
        int stage = TemperatureModel.stage(temperature, config);
        if (stage >= 2) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false, true));
        if (stage >= 3 && config.miningFatigue) player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false, true));
        if (temperature >= config.damageThreshold + 2) {
            player.setAttached(TemperatureData.LOW_TICKS, 0);
            player.setAttached(TemperatureData.DAMAGE_TICKS, 0);
        }
        if (temperature >= 40) player.setAttached(TemperatureData.WARNED, false);
        else if (!player.getAttachedOrCreate(TemperatureData.WARNED)) {
            player.setAttached(TemperatureData.WARNED, true);
            player.sendSystemMessage(Component.translatable("message.extreme_winter.return_home"));
        }
        if (temperature >= config.damageThreshold) return;
        int low = Math.min(config.damageWarningSeconds * 20, player.getAttachedOrCreate(TemperatureData.LOW_TICKS) + 20);
        player.setAttached(TemperatureData.LOW_TICKS, low);
        if (low < config.damageWarningSeconds * 20 || player.getAttachedOrCreate(TemperatureData.PROTECTION) > 0
                || player.getAttachedOrCreate(TemperatureData.FREEZE_COOLDOWN) > 0) return;
        int ticks = player.getAttachedOrCreate(TemperatureData.DAMAGE_TICKS) + 20;
        if (ticks < config.damageIntervalSeconds * 20) { player.setAttached(TemperatureData.DAMAGE_TICKS, ticks); return; }
        player.setAttached(TemperatureData.DAMAGE_TICKS, 0);
        float damage = TemperatureModel.allowedColdDamage(temperature, player.getHealth(), config);
        if (damage > 0) {
            applyingColdDamage = true;
            try { player.hurtServer(player.level(), player.damageSources().freeze(), damage); }
            finally { applyingColdDamage = false; }
        }
    }
}
