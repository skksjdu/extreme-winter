package dev.extremewinter.environment;
import java.util.Random;
import dev.extremewinter.temperature.WinterProgression;

/** Deterministic scheduling rules, independent of the world and vanilla calendar. */
public final class WinterWeatherModel {
    public enum Weather { RELIEF, SNOW, WARNING, BLIZZARD, EBB }
    public record Plan(long nextBlizzard, long duration, long event) { }
    public static final long MINUTE=WinterProgression.MINUTE;
    public static final long WARNING=5*MINUTE, EBB=2*MINUTE;
    private WinterWeatherModel() { }
    public static Weather eventAt(long now,long next,long duration) {
        if(now<next-WARNING || now>=next+duration+EBB) return null;
        if(now<next) return Weather.WARNING;
        return now<next+duration?Weather.BLIZZARD:Weather.EBB;
    }
    public static Plan firstPlan() { return new Plan(90*MINUTE,3*MINUTE,0); }
    public static Plan nextPlan(long seed,long event,long previousBlizzardEnd) {
        var random=new Random(seed ^ (event*0x9E3779B97F4A7C15L));
        return new Plan(previousBlizzardEnd+(45+random.nextInt(31))*MINUTE,
                (5+random.nextInt(3))*MINUTE,event);
    }
    public static long windowDuration(long seed,long number) {
        return (10+new Random(seed ^ number*0x632BE59BD9B4E019L).nextInt(11))*MINUTE;
    }
    public static double snowFactor(int stage,Weather weather) {
        if(weather==Weather.RELIEF) return 0;
        if(stage==0) return .25;
        return weather==Weather.BLIZZARD?2:1;
    }
    public static double freezingFactor(int stage,Weather weather) {
        return stage==0?0:snowFactor(stage,weather);
    }
    public static double airMultiplier(boolean outdoors,Weather weather) {
        return outdoors && weather==Weather.BLIZZARD?1.5:1;
    }
}
