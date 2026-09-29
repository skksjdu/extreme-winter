package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;

/** Pure rules, independent of Minecraft and the HUD. One step is one game second. */
public final class TemperatureModel {
    private TemperatureModel() { }

    public static double step(double value, boolean outdoors, boolean badWeather,
                              boolean night, boolean wet, double heat, WinterConfig config) {
        value = clamp(value, config);
        double loss = (outdoors ? config.baseLoss
                + (badWeather ? config.weatherPenalty : 0)
                + (night ? config.nighttimePenalty : 0) : 0)
                + (wet ? config.waterPenalty : 0);
        double gain = heat * config.recoveryRate;
        if (!outdoors && !wet && heat == 0 && value < config.coldThreshold) {
            gain = Math.min(config.shelteredRecoveryRate, config.coldThreshold - value);
        }
        return clamp(value + gain - loss, config);
    }

    public static double clamp(double value, WinterConfig config) {
        return Double.isFinite(value)
                ? Math.max(config.minTemperature, Math.min(config.maxTemperature, value))
                : config.maxTemperature;
    }

    public static int stage(double value, WinterConfig config) {
        if (value <= config.minTemperature) return 4;
        if (value < config.fatigueThreshold) return 3;
        if (value < config.slownessThreshold) return 2;
        if (value < config.coldThreshold) return 1;
        return 0;
    }
}
