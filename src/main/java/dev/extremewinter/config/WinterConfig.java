package dev.extremewinter.config;

/** Rates are temperature units per second (20 game ticks). Loaded once at game startup. */
public final class WinterConfig {
    public int configVersion = 3;
    public String weatherMode = "scheduled";
    public boolean blizzardParticles = true;
    public boolean blizzardWind = true;
    public boolean blizzardHaze = true;
    public boolean blizzardFrost = true;
    public boolean structureSupplies = true;
    public double minTemperature = 0;
    public double maxTemperature = 100;
    public double coldThreshold = 70;
    public double slownessThreshold = 25;
    public double fatigueThreshold = 10;
    public double baseLoss = 0.04;
    public double weatherPenalty = 0.015;
    public double nighttimePenalty = 0.01;
    public double waterPenalty = 0.20;
    public double recoveryRate = 1.2;
    public double shelteredRecoveryRate = 0.30;
    public int heatSourceRadius = 4;
    public int damageIntervalSeconds = 10;
    public double damageThreshold = 25;
    public float minimumFreezingDamage = 0.5f;
    public float freezingDamage = 1;
    public int snowIntervalTicks = 80;
    public int freezeIntervalTicks = 200;
    public int samplesPerPass = 16;
    public int simulationRadiusChunks = 4;
    public int maxSnowLayers = 16; // Zero opts into unlimited weather accumulation.
    public boolean persistentWeather = true;
    public boolean coldVanillaBiomes = true;
    public boolean coldModdedBiomes = false;
    public boolean snowAccumulation = true;
    public boolean waterFreezing = true;
    public boolean outdoorHeatExtinguishing = true;
    public int campfireExposureSeconds = 600;
    public int soulCampfireExposureSeconds = 900;
    public int furnaceExposureSeconds = 240;
    public int blastFurnaceExposureSeconds = 300;
    public int smokerExposureSeconds = 150;
    public int lavaExposureSeconds = 3600;
    public int heatRecoverySeconds = 30;
    public int torchExposureSeconds = 45;
    public int torchRecoverySeconds = 5;
    public double torchHeatStrength = 0.35;
    public double maxHeatStrength = 4;
    public int winterStageDays = 3;
    public double winterStageLossIncrease = 0.1;
    public int maxWinterStages = 10;

    public float minimumColdHealth = 6;
    public int damageWarningSeconds = 60;
    public int respawnProtectionSeconds = 180;
    public boolean miningFatigue = false;
    public boolean torchWeathering = false;
    public boolean furnaceWeathering = false;
    public boolean lavaCooling = false;

    public void validate() {
        if (!java.util.Set.of("scheduled", "legacy", "vanilla").contains(weatherMode)) throw new IllegalArgumentException("weatherMode must be scheduled, legacy or vanilla");
        finiteRange("minimumColdHealth", minimumColdHealth, 0, 20);
        finiteRange("damageWarningSeconds", damageWarningSeconds, 0, 3600);
        finiteRange("respawnProtectionSeconds", respawnProtectionSeconds, 0, 3600);
        finiteRange("heatRecoverySeconds", heatRecoverySeconds, 1, 3600);
        finiteRange("torchExposureSeconds", torchExposureSeconds, 1, 604800);
        finiteRange("torchRecoverySeconds", torchRecoverySeconds, 1, 3600);
        finiteRange("torchHeatStrength", torchHeatStrength, 0, 1);
        finiteRange("maxHeatStrength", maxHeatStrength, 1, 20);
        finiteRange("winterStageDays", winterStageDays, 1, 365);
        finiteRange("winterStageLossIncrease", winterStageLossIncrease, 0, 1);
        finiteRange("maxWinterStages", maxWinterStages, 0, 100);
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
        finiteRange("damageThreshold", damageThreshold, minTemperature, maxTemperature);
        if (damageThreshold <= minTemperature) throw new IllegalArgumentException("damageThreshold must exceed minTemperature");
        finiteRange("minimumFreezingDamage", minimumFreezingDamage, 0, 20);
        finiteRange("freezingDamage", freezingDamage, 0, 20);
        if (minimumFreezingDamage > freezingDamage) throw new IllegalArgumentException("Minimum damage exceeds maximum");
        finiteRange("snowIntervalTicks", snowIntervalTicks, 20, 72000);
        finiteRange("freezeIntervalTicks", freezeIntervalTicks, 20, 72000);
        finiteRange("samplesPerPass", samplesPerPass, 1, 64);
        finiteRange("simulationRadiusChunks", simulationRadiusChunks, 0, 8);
        finiteRange("maxSnowLayers", maxSnowLayers, 0, 4096);
        finiteRange("campfireExposureSeconds", campfireExposureSeconds, 1, 604800);
        finiteRange("soulCampfireExposureSeconds", soulCampfireExposureSeconds, 1, 604800);
        finiteRange("furnaceExposureSeconds", furnaceExposureSeconds, 1, 604800);
        finiteRange("blastFurnaceExposureSeconds", blastFurnaceExposureSeconds, 1, 604800);
        finiteRange("smokerExposureSeconds", smokerExposureSeconds, 1, 604800);
        finiteRange("lavaExposureSeconds", lavaExposureSeconds, 1, 604800);
    }

    private static void finiteRange(String name, double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
        }
    }
}
