package dev.extremewinter.environment;
import dev.extremewinter.environment.WinterWeatherModel.Weather;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.temperature.TemperatureModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WinterWeatherModelTest {
    @Test void firstWarningBlizzardAndEbbBoundariesAreExact() {
        var p=WinterWeatherModel.firstPlan();
        assertEquals(90*1200,p.nextBlizzard()); assertEquals(3*1200,p.duration());
        assertNull(WinterWeatherModel.eventAt(85*1200-1,p.nextBlizzard(),p.duration()));
        assertEquals(Weather.WARNING,WinterWeatherModel.eventAt(85*1200,p.nextBlizzard(),p.duration()));
        assertEquals(Weather.WARNING,WinterWeatherModel.eventAt(90*1200-1,p.nextBlizzard(),p.duration()));
        assertEquals(Weather.BLIZZARD,WinterWeatherModel.eventAt(90*1200,p.nextBlizzard(),p.duration()));
        assertEquals(Weather.BLIZZARD,WinterWeatherModel.eventAt(93*1200-1,p.nextBlizzard(),p.duration()));
        assertEquals(Weather.EBB,WinterWeatherModel.eventAt(93*1200,p.nextBlizzard(),p.duration()));
        assertNull(WinterWeatherModel.eventAt(95*1200,p.nextBlizzard(),p.duration()));
    }
    @Test void futurePlansAndWindowsAreDeterministicAndBounded() {
        for(long seed=0;seed<100;seed++) for(int event=1;event<=5;event++) {
            var p=WinterWeatherModel.nextPlan(seed,event,93*1200);
            assertEquals(p,WinterWeatherModel.nextPlan(seed,event,93*1200));
            assertTrue(p.nextBlizzard()>=138*1200 && p.nextBlizzard()<=168*1200);
            assertTrue(p.duration()>=5*1200 && p.duration()<=7*1200);
            long window=WinterWeatherModel.windowDuration(seed,event);
            assertTrue(window>=10*1200 && window<=20*1200);
        }
    }
    @Test void samplingFactorsRespectPreparationReliefAndFixedBudget() {
        assertEquals(.25,WinterWeatherModel.snowFactor(0,Weather.SNOW));
        assertEquals(0,WinterWeatherModel.freezingFactor(0,Weather.SNOW));
        assertEquals(0,WinterWeatherModel.snowFactor(4,Weather.RELIEF));
        assertEquals(1,WinterWeatherModel.snowFactor(1,Weather.SNOW));
        assertEquals(2,WinterWeatherModel.snowFactor(1,Weather.BLIZZARD));
    }
    @Test void blizzardAmplifiesAirButDoesNotAmplifyWaterOrIndoorRecovery() {
        var c=new WinterConfig();
        assertEquals(1,WinterWeatherModel.airMultiplier(false,Weather.BLIZZARD));
        assertEquals(1.5,WinterWeatherModel.airMultiplier(true,Weather.BLIZZARD));
        assertEquals(80-(.065*1.5+.2)*1.25,TemperatureModel.step(80,true,true,true,true,0,1.25,1.5,c),1e-8);
        assertEquals(80-.2*1.25,TemperatureModel.step(80,false,true,true,true,0,1.25,1,c),1e-8);
        assertEquals(50.3,TemperatureModel.step(50,false,true,true,false,0,1.25,1,c),1e-8);
    }
    @Test void invalidWeatherModesAreRejected() {
        var c=new WinterConfig();c.weatherMode="unknown";
        assertThrows(IllegalArgumentException.class,c::validate);
    }
}
