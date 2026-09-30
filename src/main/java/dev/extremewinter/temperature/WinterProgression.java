package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;

/** Calendar time follows vanilla game days, including nights skipped by sleeping. */
public final class WinterProgression {
    private WinterProgression() { }

    public static int stage(long timeOfDay, WinterConfig config) {
        long days = Math.max(0, timeOfDay) / 24000;
        return (int) Math.min(config.maxWinterStages, days / config.winterStageDays);
    }

    public static double lossMultiplier(long timeOfDay, WinterConfig config) {
        return 1 + stage(timeOfDay, config) * config.winterStageLossIncrease;
    }
}
