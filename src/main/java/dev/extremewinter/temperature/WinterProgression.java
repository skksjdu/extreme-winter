package dev.extremewinter.temperature;
import dev.extremewinter.config.WinterConfig;

/** Running ticks only; vanilla calendar commands and sleep cannot advance winter. */
public final class WinterProgression {
    public static final long MINUTE = 1200;
    private static final int[] BOUNDARIES = {60, 120, 180, 270};
    private static final double[] MULTIPLIERS = {.65, .80, 1, 1.10, 1.25};
    private WinterProgression() { }
    public static int stage(long elapsedTicks, WinterConfig config) {
        int stage = 0;
        while (stage < BOUNDARIES.length && elapsedTicks >= BOUNDARIES[stage] * MINUTE) stage++;
        return stage;
    }
    public static double lossMultiplier(long elapsedTicks, WinterConfig config) {
        return MULTIPLIERS[stage(elapsedTicks, config)];
    }
}
