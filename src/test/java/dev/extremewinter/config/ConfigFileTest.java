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
        String json = "{\"baseLoss\": 0.12, \"persistentWeather\": false}";
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
                "{\"heatSourceRadius\":999}", "{\"coldThreshold\":10}", "{\"baseLoss\":\"NaN\"}"}) {
            Files.writeString(path, invalid);
            assertEquals(0.06, ConfigFile.load(path).baseLoss);
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
}
