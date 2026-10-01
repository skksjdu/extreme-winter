package dev.extremewinter.temperature;
import dev.extremewinter.config.WinterConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemperatureModelTest {
    private final WinterConfig config = new WinterConfig();
    @Test void snowDayGivesMoreThanTwentyTwoMinutesBeforeWarning() {
        double value = 100;
        for (int seconds = 0; seconds < 1363; seconds++)
            value = TemperatureModel.step(value, true, true, false, false, 0, config);
        assertTrue(value >= 25);
        assertTrue(TemperatureModel.step(value, true, true, false, false, 0, config) < 25);
    }
    @Test void rainAndNightOnlyPenalizeExposureButWaterPenalizesIndoorsToo() {
        assertEquals(80, TemperatureModel.step(80, false, true, true, false, 0, config));
        assertEquals(79.8, TemperatureModel.step(80, false, true, true, true, 0, config), 1e-8);
        assertEquals(79.935, TemperatureModel.step(80, true, true, true, false, 0, config), 1e-8);
    }
    @Test void shelterIsSafeAndPassiveRecoveryStopsAtColdThreshold() {
        assertEquals(70, TemperatureModel.step(69.99, false, false, false, false, 0, config));
        assertEquals(80, TemperatureModel.step(80, false, false, false, false, 0, config));
        double value=40;
        for (int i=0;i<100;i++) value=TemperatureModel.step(value,false,false,false,false,0,config);
        assertEquals(70,value,1e-8);
    }
    @Test void invalidSavedValuesAreRepairedAndBoundsHold() {
        assertEquals(100, TemperatureModel.clamp(Double.NaN, config));
        assertEquals(100, TemperatureModel.clamp(Double.POSITIVE_INFINITY, config));
        assertEquals(0, TemperatureModel.step(-1, true, true, true, true, 0, config));
        assertEquals(100, TemperatureModel.step(99.9, false, false, false, false, 1, config));
    }
    @Test void thresholdsHaveNoGaps() {
        assertEquals(0, TemperatureModel.stage(70, config));
        assertEquals(1, TemperatureModel.stage(25, config));
        assertEquals(2, TemperatureModel.stage(10, config));
        assertEquals(3, TemperatureModel.stage(.01, config));
        assertEquals(4, TemperatureModel.stage(0, config));
    }
    @Test void heatRecoversAccordingToCombinedSourceStrength() {
        assertEquals(51.2, TemperatureModel.step(50,false,false,false,false,1,config),1e-8);
        assertEquals(50.3, TemperatureModel.step(50,false,false,false,false,.25,config),1e-8);
        assertEquals(52.4, TemperatureModel.step(50,false,false,false,false,2,config),1e-8);
        assertTrue(TemperatureModel.step(50,true,true,true,false,1,config)>50);
    }
    @Test void stagesUseSixtyOneHundredTwentyOneHundredEightyAndTwoHundredSeventyMinutes() {
        int[] minutes={60,120,180,270}; double[] multipliers={.65,.8,1,1.1,1.25};
        for(int i=0;i<minutes.length;i++) {
            assertEquals(i,WinterProgression.stage(minutes[i]*1200L-1,config));
            assertEquals(i+1,WinterProgression.stage(minutes[i]*1200L,config));
            assertEquals(multipliers[i],WinterProgression.lossMultiplier(minutes[i]*1200L-1,config));
        }
        assertEquals(.65,WinterProgression.lossMultiplier(-1,config));
        assertEquals(1.25,WinterProgression.lossMultiplier(Long.MAX_VALUE,config));
    }
    @Test void lateWinterSpeedsLossButDryShelterIsStillSafe() {
        double multiplier=WinterProgression.lossMultiplier(270*1200L,config);
        assertEquals(79.93125,TemperatureModel.step(80,true,true,false,false,0,multiplier,config),1e-8);
        assertEquals(80,TemperatureModel.step(80,false,true,true,false,0,multiplier,config));
        assertEquals(79.75,TemperatureModel.step(80,false,false,false,true,0,multiplier,config),1e-8);
    }
    @Test void freezingStartsBelowTwentyFiveWithHalfToOnePointPulses() {
        assertEquals(0,TemperatureModel.freezingDamage(25,config));
        assertTrue(TemperatureModel.freezingDamage(24.99,config)>=.5);
        assertEquals(.75f,TemperatureModel.freezingDamage(12.5,config));
        assertEquals(1,TemperatureModel.freezingDamage(0,config));
        assertTrue(TemperatureModel.freezingDamage(5,config)>TemperatureModel.freezingDamage(20,config));
    }
    @Test void customTemperatureScaleUsesConfiguredDamageThreshold() {
        config.minTemperature=-20; config.damageThreshold=20;
        assertEquals(0,TemperatureModel.freezingDamage(20,config));
        assertEquals(.75f,TemperatureModel.freezingDamage(0,config));
        assertEquals(1,TemperatureModel.freezingDamage(-20,config));
    }
    @Test void coldNeverCrossesSixHealthOrHealsExistingLowHealth() {
        assertEquals(1,TemperatureModel.allowedColdDamage(0,20,config));
        assertEquals(.25f,TemperatureModel.allowedColdDamage(0,6.25f,config));
        assertEquals(0,TemperatureModel.allowedColdDamage(0,6,config));
        assertEquals(0,TemperatureModel.allowedColdDamage(0,4,config));
    }
}
