package dev.extremewinter.temperature;

import dev.extremewinter.config.WinterConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WinterGearModelTest {
    @Test void completeAndMixedSetsHavePredictableAirProtection() {
        assertEquals(1, TemperatureModel.gearAirMultiplier(0, 0, false), 1e-10);
        assertEquals(.85, TemperatureModel.gearAirMultiplier(1, 0, false), 1e-10);
        assertEquals(.65, TemperatureModel.gearAirMultiplier(0, 1, false), 1e-10);
        assertEquals(.50, TemperatureModel.gearAirMultiplier(1, 1, false), 1e-10);
        assertEquals(.86, TemperatureModel.gearAirMultiplier(0, .4, false), 1e-10);
        assertEquals(.4, TemperatureModel.gearAirMultiplier(1, 1, true), 1e-10);
    }
    @Test void wetPenaltyIsIndependentOfArmorFoodAndBlizzard() {
        var c = new WinterConfig();
        double multiplier = TemperatureModel.gearAirMultiplier(1, 1, true) * 1.5;
        assertEquals(80 - ((.04 + .015 + .01) * multiplier + .20) * 1.25,
                TemperatureModel.step(80, true, true, true, true, 0, 1.25, multiplier, c), 1e-10);
    }
    @Test void invalidOrExcessWeightsCannotProduceNegativeOrInfiniteLoss() {
        assertEquals(.4, TemperatureModel.gearAirMultiplier(100, 100, true), 1e-10);
        assertEquals(.8, TemperatureModel.gearAirMultiplier(Double.NaN, Double.POSITIVE_INFINITY, true), 1e-10);
        assertEquals(1, TemperatureModel.gearAirMultiplier(-1, -1, false), 1e-10);
    }
}
