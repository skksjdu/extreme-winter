package dev.extremewinter.environment;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.temperature.WinterWorldState;
import dev.extremewinter.temperature.WinterProgression;
import dev.extremewinter.network.WinterStatusSync;
import dev.extremewinter.environment.WinterWeatherModel.Weather;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/** Sole owner of scheduled vanilla weather. Legacy and vanilla are explicit alternatives. */
public final class WinterWeatherController {
    private final WinterConfig config;
    public WinterWeatherController(WinterConfig config) { this.config=config; }
    public void onLoad(ServerLevel world) {
        if(world.dimension().equals(Level.OVERWORLD) && config.weatherMode.equals("legacy") && config.persistentWeather) legacyRain(world);
    }
    private static void legacyRain(ServerLevel world) {
        var data=world.getWeatherData();data.setClearWeatherTime(0);data.setRainTime(12000);data.setThunderTime(12000);
        data.setRaining(true);data.setThundering(false);
    }
    public void tick(MinecraftServer server) {
        var world=server.overworld();var state=WinterWorldState.get(world);
        if(config.weatherMode.equals("scheduled")) {
            int before=(int)state.value("weatherKind",-1);
            update(world);
            boolean changed=before!=state.value("weatherKind",-1);
            if(changed || server.getTickCount()%20==0) applyVanilla(world);
            if(changed || server.getTickCount()%200==0) for(var p:server.getPlayerList().getPlayers()) {
                WinterStatusSync.send(p,config);
                if(changed && current(world)==Weather.WARNING && !p.isSpectator())
                    p.sendSystemMessage(Component.translatable("message.extreme_winter.blizzard_warning"));
            }
        } else {
            if(config.weatherMode.equals("legacy") && config.persistentWeather && server.getTickCount()%1200==0) legacyRain(world);
            if(server.getTickCount()%200==0) for(var p:server.getPlayerList().getPlayers()) WinterStatusSync.send(p,config);
        }
    }
    public void update(ServerLevel world) {
        var s=WinterWorldState.get(world);long now=s.elapsedTicks();
        if(s.value("weatherInitialized",0)==0) {
            var first=WinterWeatherModel.firstPlan();
            s.put("weatherRandomSeed",world.getSeed() ^ 0x51A7E123L);
            s.put("nextBlizzardTick",first.nextBlizzard());s.put("blizzardDurationTicks",first.duration());
            s.put("eventNumber",0);s.put("windowNumber",0);s.put("windowKind",0);
            s.put("windowEndTick",WinterWeatherModel.windowDuration(s.value("weatherRandomSeed",0),0));
            s.put("weatherInitialized",1);
        }
        long seed=s.value("weatherRandomSeed",0),next=s.value("nextBlizzardTick",90*1200),duration=s.value("blizzardDurationTicks",3*1200);
        while(now>=next+duration+WinterWeatherModel.EBB) {
            long previousEnd=next+duration;
            var plan=WinterWeatherModel.nextPlan(seed,s.value("eventNumber",0)+1,previousEnd);
            next=plan.nextBlizzard();duration=plan.duration();
            s.put("eventNumber",plan.event());s.put("nextBlizzardTick",next);s.put("blizzardDurationTicks",duration);
            long window=s.value("windowNumber",0)+1;s.put("windowNumber",window);s.put("windowKind",0);
            s.put("windowEndTick",previousEnd+WinterWeatherModel.EBB+WinterWeatherModel.windowDuration(seed,window));
        }
        long windowEnd=s.value("windowEndTick",0);
        while(now>=windowEnd) {
            long number=s.value("windowNumber",0)+1;
            s.put("windowNumber",number);s.put("windowKind",1-s.value("windowKind",0));
            windowEnd+=WinterWeatherModel.windowDuration(seed,number);s.put("windowEndTick",windowEnd);
        }
        Weather event=WinterWeatherModel.eventAt(now,next,duration);
        Weather weather=event!=null?event:s.value("windowKind",0)==0?Weather.RELIEF:Weather.SNOW;
        // Preparation can never expose a player to an event, including malformed past schedules.
        if(WinterProgression.stage(now,config)==0 && weather.ordinal()>=Weather.WARNING.ordinal()) weather=Weather.SNOW;
        long end=switch(weather) {case WARNING->next;case BLIZZARD->next+duration;case EBB->next+duration+WinterWeatherModel.EBB;default->Math.min(windowEnd,next-WinterWeatherModel.WARNING);};
        if(s.value("weatherKind",-1)!=weather.ordinal()) s.put("weatherStartTick",now);
        s.put("weatherKind",weather.ordinal());s.put("weatherEndTick",end);
    }
    public void applyVanilla(ServerLevel world) {
        Weather weather=current(world);boolean rain=weather!=Weather.RELIEF;
        var data=world.getWeatherData();
        data.setClearWeatherTime(rain?0:1200);data.setRainTime(1200);data.setThunderTime(1200);
        data.setRaining(rain);data.setThundering(false);
    }
    public static Weather current(ServerLevel world) {
        int ordinal=(int)WinterWorldState.get(world).value("weatherKind",0);
        return Weather.values()[Math.clamp(ordinal,0,Weather.values().length-1)];
    }
    public static double snowFactor(ServerLevel world,WinterConfig c) {
        if(!c.weatherMode.equals("scheduled")) return world.isRaining()?1:0;
        return WinterWeatherModel.snowFactor(WinterProgression.stage(WinterWorldState.get(world).elapsedTicks(),c),current(world));
    }
    public static double freezingFactor(ServerLevel world,WinterConfig c) {
        if(!c.weatherMode.equals("scheduled")) return world.isRaining()?1:0;
        return WinterWeatherModel.freezingFactor(WinterProgression.stage(WinterWorldState.get(world).elapsedTicks(),c),current(world));
    }
    public static double airMultiplier(ServerLevel world,boolean outdoors,WinterConfig c) {
        return c.weatherMode.equals("scheduled")?WinterWeatherModel.airMultiplier(outdoors,current(world)):1;
    }
}
