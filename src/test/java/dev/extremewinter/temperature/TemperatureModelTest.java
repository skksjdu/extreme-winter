package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemperatureModelTest {
    private final WinterConfig config = new WinterConfig();

    @Test void daytimeExplorationHasMoreThanTenMinutesBeforeSlowness() {
        double value = 100;
        for (int seconds = 0; seconds < 600; seconds++) {
            value = TemperatureModel.step(value, true, true, false, false, 0, config);
        }
        assertEquals(40, value, 1e-8);
    }

    @Test void rainAndNightOnlyPenalizeExposureButWaterPenalizesIndoorsToo() {
        assertEquals(80, TemperatureModel.step(80, false, true, true, false, 0, config));
        assertEquals(79.4, TemperatureModel.step(80, false, true, true, true, 0, config), 1e-8);
        assertEquals(79.87, TemperatureModel.step(80, true, true, true, false, 0, config), 1e-8);
    }

    @Test void shelterIsSafeAndPassiveRecoveryStopsAtColdThreshold() {
        assertEquals(70, TemperatureModel.step(69.99, false, false, false, false, 0, config));
        assertEquals(80, TemperatureModel.step(80, false, false, false, false, 0, config));
    }

    @Test void invalidSavedValuesAreRepairedAndBoundsHold() {
        assertEquals(100, TemperatureModel.clamp(Double.NaN, config));
        assertEquals(100, TemperatureModel.clamp(Double.POSITIVE_INFINITY, config));
        assertEquals(0, TemperatureModel.step(-1, true, true, true, true, 0, config));
        assertEquals(100, TemperatureModel.step(99.9, false, false, false, false, 1, config));
    }

    @Test void thresholdsHaveNoGaps() {
        assertEquals(0, TemperatureModel.stage(70, config));
        assertEquals(1, TemperatureModel.stage(40, config));
        assertEquals(2, TemperatureModel.stage(20, config));
        assertEquals(3, TemperatureModel.stage(0.01, config));
        assertEquals(4, TemperatureModel.stage(0, config));
    }

    @Test void heatRecoversGraduallyAndNeverStacksBeyondTheStrongestSource() {
        assertEquals(51.2, TemperatureModel.step(50, false, false, false, false, 1, config), 1e-8);
        assertEquals(50.3, TemperatureModel.step(50, false, false, false, false, 0.25, config), 1e-8);
        assertTrue(TemperatureModel.step(50, true, true, true, false, 0.25, config) > 50);
    }
}
