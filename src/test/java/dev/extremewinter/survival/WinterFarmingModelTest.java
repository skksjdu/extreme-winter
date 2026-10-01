package dev.extremewinter.survival;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WinterFarmingModelTest {
    @Test void vanillaForPreparationAndOtherDimensions() {
        for (int stage = 0; stage < 3; stage++) assertEquals(1, WinterFarmingModel.growthChance(stage, true, false));
        assertEquals(1, WinterFarmingModel.growthChance(4, false, false));
    }
    @Test void deepWinterAndCappedLongWinter() {
        assertEquals(.5, WinterFarmingModel.growthChance(3, true, false));
        assertEquals(.25, WinterFarmingModel.growthChance(4, true, false));
        assertEquals(.25, WinterFarmingModel.growthChance(Integer.MAX_VALUE, true, false));
    }
    @Test void greenhouseRestoresVanillaProbability() {
        assertEquals(1, WinterFarmingModel.growthChance(3, true, true));
        assertEquals(1, WinterFarmingModel.growthChance(4, true, true));
    }
}
