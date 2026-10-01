package dev.extremewinter.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigFileTest {
    @TempDir Path directory;

    @Test void missingFileCreatesReadableDefaults() throws Exception {
        Path path = directory.resolve("winter.json");
        var config = ConfigFile.load(path);
        config.validate();
        assertTrue(Files.readString(path).contains("\n  \"baseLoss\""));
        assertEquals(100, config.maxTemperature);
        assertEquals(16, config.maxSnowLayers);
    }

    @Test void partialConfigPreservesDefaultsAndUserFile() throws Exception {
        Path path = directory.resolve("winter.json");
        String json = "{\"configVersion\": 3, \"baseLoss\": 0.12, \"persistentWeather\": false}";
        Files.writeString(path, json);
        var config = ConfigFile.load(path);
        assertEquals(0.12, config.baseLoss);
        assertEquals(4, config.heatSourceRadius);
        assertFalse(config.persistentWeather);
        assertEquals(json, Files.readString(path));
    }

    @Test void invalidConfigFallsBackWithoutOverwriting() throws Exception {
        Path path = directory.resolve("winter.json");
        for (String invalid : new String[]{"{broken", "null", "{\"snowIntervalTicks\":0}",
                "{\"heatSourceRadius\":999}", "{\"coldThreshold\":10}", "{\"baseLoss\":\"NaN\"}",
                "{\"lavaExposureSeconds\":0}", "{\"campfireExposureSeconds\":-1}", "{\"heatRecoverySeconds\":0}",
                "{\"torchExposureSeconds\":0}", "{\"torchRecoverySeconds\":0}", "{\"winterStageDays\":0}",
                "{\"maxWinterStages\":-1}", "{\"maxHeatStrength\":0.5}", "{\"maxSnowLayers\":-1}"}) {
            Files.writeString(path, invalid);
            var loaded = ConfigFile.load(path);
            assertEquals(0.04, loaded.baseLoss);
            assertEquals(16, loaded.maxSnowLayers);
            loaded.validate();
            assertEquals(invalid, Files.readString(path));
        }
    }

    @Test void reorderedOrInfiniteThresholdsAreRejected() {
        var config = new WinterConfig();
        config.fatigueThreshold = 80;
        assertThrows(IllegalArgumentException.class, config::validate);
        config = new WinterConfig();
        config.maxTemperature = Double.POSITIVE_INFINITY;
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test void legacyDefaultsUpgradeWithBackupAndCustomValuesSurvive() throws Exception {
        Path path = directory.resolve("winter.json");
        String original = "{\"baseLoss\":0.06,\"weatherPenalty\":0.04,\"waterPenalty\":2.5,"
                + "\"maxSnowLayers\":3,\"freezingDamage\":1,\"extra\":\"keep\"}";
        Files.writeString(path, original);
        var config = ConfigFile.load(path);
        assertEquals(0.04, config.baseLoss);
        assertEquals(0.015, config.weatherPenalty);
        assertEquals(2.5, config.waterPenalty);
        assertEquals(0, config.maxSnowLayers);
        assertEquals(1, config.freezingDamage);
        assertEquals(original, Files.readString(directory.resolve("winter.json.v1.bak")));
        assertTrue(Files.readString(path).contains("keep"));
        String upgraded = Files.readString(path);
        ConfigFile.load(path);
        assertEquals(upgraded, Files.readString(path));
    }

    @Test void v2MigrationRetainsUnknownClimateAndUnlimitedSnowAndBacksUpOnlyOnce() throws Exception {
        Path path=directory.resolve("winter.json");
        String source="{\"configVersion\":2,\"baseLoss\":0.32,\"slownessThreshold\":40,\"fatigueThreshold\":20,\"damageThreshold\":40,\"freezingDamage\":6,\"minimumFreezingDamage\":1,\"damageIntervalSeconds\":4,\"maxSnowLayers\":0,\"coldModdedBiomes\":true,\"extra\":{\"keep\":1}}";
        Files.writeString(path,source);
        var config=ConfigFile.load(path);
        assertEquals(3,config.configVersion); assertEquals(.04,config.baseLoss);
        assertEquals(25,config.slownessThreshold); assertEquals(10,config.fatigueThreshold);
        assertEquals(.5,config.minimumFreezingDamage); assertEquals(1,config.freezingDamage);
        assertEquals(10,config.damageIntervalSeconds); assertEquals(0,config.maxSnowLayers);
        assertTrue(config.coldModdedBiomes); assertTrue(Files.readString(path).contains("keep"));
        assertEquals(source,Files.readString(directory.resolve("winter.json.v2.bak")));
        String migrated=Files.readString(path); ConfigFile.load(path);
        assertEquals(migrated,Files.readString(path));
    }
    @Test void customThresholdAndDamageGroupsStayConsistent() throws Exception {
        Path path=directory.resolve("winter.json");
        Files.writeString(path,"{\"configVersion\":2,\"slownessThreshold\":35,\"freezingDamage\":0,\"baseLoss\":0.12}");
        var c=ConfigFile.load(path); c.validate();
        assertEquals(35,c.slownessThreshold); assertEquals(20,c.fatigueThreshold);
        assertEquals(35,c.damageThreshold); assertEquals(0,c.minimumFreezingDamage);
        assertEquals(0,c.freezingDamage); assertEquals(.12,c.baseLoss);
    }
    @Test void invalidLegacyFileNeverCreatesAMigrationBackup() throws Exception {
        Path path=directory.resolve("winter.json");
        String source="{\"configVersion\":2,\"fatigueThreshold\":90}";
        Files.writeString(path,source); ConfigFile.load(path);
        assertEquals(source,Files.readString(path)); assertFalse(Files.exists(directory.resolve("winter.json.v2.bak")));
    }
    @Test void legacyDisabledDamageIsRetained() throws Exception {
        Path path = directory.resolve("winter.json");
        Files.writeString(path, "{\"freezingDamage\":0}");
        var config = ConfigFile.load(path);
        assertEquals(0, config.freezingDamage);
        assertEquals(0, config.minimumFreezingDamage);
    }

    @Test void explicitSnowLimitsAndUnlimitedOptInArePreserved() throws Exception {
        Path path = directory.resolve("winter.json");
        for (int cap : new int[]{0, 64, 4096}) {
            String json = "{\"configVersion\":3,\"maxSnowLayers\":" + cap + "}";
            Files.writeString(path, json);
            assertEquals(cap, ConfigFile.load(path).maxSnowLayers);
            assertEquals(json, Files.readString(path));
        }
    }

    @Test void moddedClimateIsOptInAndIndependentOfVanillaClimate() throws Exception {
        Path path = directory.resolve("winter.json");
        String legacy = "{\"configVersion\":3,\"coldVanillaBiomes\":true}";
        Files.writeString(path, legacy);
        assertFalse(ConfigFile.load(path).coldModdedBiomes);
        assertEquals(legacy, Files.readString(path));
        String custom = "{\"configVersion\":3,\"coldVanillaBiomes\":false,\"coldModdedBiomes\":true,\"extra\":\"keep\"}";
        Files.writeString(path, custom);
        var config = ConfigFile.load(path);
        assertFalse(config.coldVanillaBiomes);
        assertTrue(config.coldModdedBiomes);
        assertEquals(custom, Files.readString(path));
    }

    @Test void oldDisabledPersistentWeatherKeepsVanillaOwnershipAndExplicitModesSurvive() throws Exception {
        Path path=directory.resolve("winter.json");
        Files.writeString(path,"{\"configVersion\":2,\"persistentWeather\":false}");
        assertEquals("vanilla",ConfigFile.load(path).weatherMode);
        Files.writeString(path,"{\"configVersion\":3,\"persistentWeather\":true,\"weatherMode\":\"legacy\"}");
        assertEquals("legacy",ConfigFile.load(path).weatherMode);
    }
}
