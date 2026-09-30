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
    }

    @Test void partialConfigPreservesDefaultsAndUserFile() throws Exception {
        Path path = directory.resolve("winter.json");
        String json = "{\"configVersion\": 2, \"baseLoss\": 0.12, \"persistentWeather\": false}";
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
                "{\"lavaExposureSeconds\":0}", "{\"campfireExposureSeconds\":-1}", "{\"heatRecoverySeconds\":0}"}) {
            Files.writeString(path, invalid);
            assertEquals(0.32, ConfigFile.load(path).baseLoss);
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
        assertEquals(0.32, config.baseLoss);
        assertEquals(0.18, config.weatherPenalty);
        assertEquals(2.5, config.waterPenalty);
        assertEquals(64, config.maxSnowLayers);
        assertEquals(6, config.freezingDamage);
        assertEquals(original, Files.readString(directory.resolve("winter.json.v1.bak")));
        assertTrue(Files.readString(path).contains("keep"));
        String upgraded = Files.readString(path);
        ConfigFile.load(path);
        assertEquals(upgraded, Files.readString(path));
    }

    @Test void legacyDisabledDamageIsRetained() throws Exception {
        Path path = directory.resolve("winter.json");
        Files.writeString(path, "{\"freezingDamage\":0}");
        var config = ConfigFile.load(path);
        assertEquals(0, config.freezingDamage);
        assertEquals(0, config.minimumFreezingDamage);
    }
}
