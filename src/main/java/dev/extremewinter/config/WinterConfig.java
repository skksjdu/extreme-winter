package dev.extremewinter.config;

/** Rates are temperature units per second (20 game ticks). Loaded once at game startup. */
public final class WinterConfig {
    public double minTemperature = 0;
    public double maxTemperature = 100;
    public double coldThreshold = 70;
    public double slownessThreshold = 40;
    public double fatigueThreshold = 20;
    public double baseLoss = 0.06;
    public double weatherPenalty = 0.04;
    public double nighttimePenalty = 0.03;
    public double waterPenalty = 0.6;
    public double recoveryRate = 1.2;
    public double shelteredRecoveryRate = 0.03;
    public int heatSourceRadius = 4;
    public int damageIntervalSeconds = 5;
    public float freezingDamage = 1;
    public int snowIntervalTicks = 20;
    public int freezeIntervalTicks = 40;
    public int samplesPerPass = 16;
    public int simulationRadiusChunks = 4;
    public int maxSnowLayers = 3;
    public boolean persistentWeather = true;
    public boolean coldVanillaBiomes = true;
    public boolean starterShelter = true;
    public boolean snowAccumulation = true;
    public boolean waterFreezing = true;

    public void validate() {
        finiteRange("minTemperature", minTemperature, -10000, 10000);
        finiteRange("maxTemperature", maxTemperature, -10000, 10000);
        if (!(minTemperature < fatigueThreshold && fatigueThreshold < slownessThreshold
                && slownessThreshold < coldThreshold && coldThreshold <= maxTemperature)) {
            throw new IllegalArgumentException("Thresholds must satisfy min < fatigue < slowness < cold <= max");
        }
        finiteRange("fatigueThreshold", fatigueThreshold, minTemperature, maxTemperature);
        finiteRange("slownessThreshold", slownessThreshold, minTemperature, maxTemperature);
        finiteRange("coldThreshold", coldThreshold, minTemperature, maxTemperature);
        finiteRange("baseLoss", baseLoss, 0, 100);
        finiteRange("weatherPenalty", weatherPenalty, 0, 100);
        finiteRange("nighttimePenalty", nighttimePenalty, 0, 100);
        finiteRange("waterPenalty", waterPenalty, 0, 100);
        finiteRange("recoveryRate", recoveryRate, 0, 100);
        finiteRange("shelteredRecoveryRate", shelteredRecoveryRate, 0, 100);
        finiteRange("heatSourceRadius", heatSourceRadius, 1, 6);
        finiteRange("damageIntervalSeconds", damageIntervalSeconds, 1, 3600);
        finiteRange("freezingDamage", freezingDamage, 0, 20);
        finiteRange("snowIntervalTicks", snowIntervalTicks, 20, 72000);
        finiteRange("freezeIntervalTicks", freezeIntervalTicks, 20, 72000);
        finiteRange("samplesPerPass", samplesPerPass, 1, 64);
        finiteRange("simulationRadiusChunks", simulationRadiusChunks, 0, 8);
        finiteRange("maxSnowLayers", maxSnowLayers, 1, 8);
    }

    private static void finiteRange(String name, double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
    }
}
