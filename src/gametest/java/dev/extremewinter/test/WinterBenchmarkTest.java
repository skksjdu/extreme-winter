package dev.extremewinter.test;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import dev.extremewinter.test.metrics.WinterWorkProfiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;

/** Opt-in production-JAR benchmark; isolated test mixins time real mod work without shipping a profiler. */
public final class WinterBenchmarkTest implements FabricClientGameTest {
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    private final List<Map<String,Object>> results=new ArrayList<>();
    public void runTest(ClientGameTestContext context){
        var cfg=ExtremeWinter.CONFIG; boolean oldHaze=cfg.blizzardHaze,oldFrost=cfg.blizzardFrost,oldParticles=cfg.blizzardParticles,oldWind=cfg.blizzardWind;
        String oldWeather=cfg.weatherMode;cfg.weatherMode="scheduled";
        ServerTickEvents.END_SERVER_TICK.register(s->WinterWorkProfiler.finishTick());
        try(var game=context.worldBuilder().adjustSettings(c->{c.setSeed("20261001");c.getGameRules().set(GameRules.RANDOM_TICK_SPEED,3,null);}).create()){
            game.getServer().runOnServer(s->{var w=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                for(int x=16;x<48;x++)for(int z=16;z<48;z++)w.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                for(int x=20;x<44;x++)for(int z=20;z<44;z++)if(z<32)w.setBlock(new BlockPos(x,104,z),Blocks.GLASS.defaultBlockState(),3);
                for(int x=22;x<=40;x+=6)for(int z=22;z<=28;z+=6){var pos=new BlockPos(x,100,z);w.setBlock(pos,HeatingContent.STOVE.defaultBlockState(),3);((HeatingStoveBlockEntity)w.getBlockEntity(pos)).setItem(0,new ItemStack(Items.COAL_BLOCK,4));}
                int crops=0;for(int x=22;x<=40;x++)for(int z=23;z<=27;z++){var pos=new BlockPos(x,100,z);w.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);w.setBlock(pos,Blocks.WHEAT.defaultBlockState(),3);crops++;}
                p.teleportTo(w,32.5,100,38.5,Set.of(),180,0,true);TemperatureData.set(p,100);
                ExtremeWinter.LOGGER.info("TEST E benchmark fixture seed=20261001 renderDistance=8 simulationDistance=5 resolution=854x480 crops={} stoves=8 native randomTick=3",crops);
            });
            context.runOnClient(c->{c.options.renderDistance().set(8);c.options.simulationDistance().set(5);c.options.framerateLimit().set(260);c.options.enableVsync().set(false);c.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);c.options.hideGui=false;});
            context.waitTicks(200);
            context.waitFor(c -> {
                for (int x=1;x<=2;x++) for(int z=1;z<=2;z++) if(c.level.getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null)return false;
                return true;
            });
            // The API's default download predicate expects a square view radius, while native transmission is rounded.
            // Check every actual scene chunk above, and still wait for the renderer/light queues to finish here.
            game.getClientLevel().waitForChunksRender(false);
            // Actual elapsed-time segments with the unmodified 60/270 minute thresholds.
            if (!Boolean.getBoolean("winter.test.skipClockSegments")) {
                clock(context,game,"preparation-start",0,0,0);
                clock(context,game,"one-hour-boundary",60*1200-600,0,1);
                clock(context,game,"long-winter-boundary",270*1200-600,3,4);
            } else game.getServer().runOnServer(s->WinterWorldState.get(s.overworld()).setElapsedTicks(270*1200));
            // Fixed synthetic weather placement keeps all four visual samples in the same long-winter blizzard.
            game.getServer().runOnServer(s->{var w=s.overworld();var state=WinterWorldState.get(w);state.put("nextBlizzardTick",state.elapsedTicks());state.put("blizzardDurationTicks",7*1200);
                var controller=new WinterWeatherController(cfg);controller.update(w);controller.applyVanilla(w);TemperatureData.set(s.getPlayerList().getPlayers().getFirst(),10);});
            for(int i=0;i<4;i++){
                boolean enabled=i%2==1;cfg.blizzardHaze=enabled;cfg.blizzardFrost=enabled;cfg.blizzardParticles=enabled;cfg.blizzardWind=enabled;
                context.waitTicks(100);sample(context,game,"visual-"+(enabled?"on":"off")+"-"+(i/2+1),600);
            }
            game.getServer().runOnServer(s->{var w=s.overworld();int snow=0;for(int x=16;x<48;x++)for(int z=32;z<48;z++)snow+=dev.extremewinter.environment.SnowDriftBlock.layers(w.getBlockState(new BlockPos(x,100,z)));
                require(snow>0,"fixed scene acquires actual snow while profiling");ExtremeWinter.LOGGER.info("TEST E benchmark actual exposed snow layers={}",snow);});
            context.takeScreenshot("beginner-E-benchmark-fixed-scene");
        }finally{cfg.blizzardHaze=oldHaze;cfg.blizzardFrost=oldFrost;cfg.blizzardParticles=oldParticles;cfg.blizzardWind=oldWind;cfg.weatherMode=oldWeather;WinterWorkProfiler.disable();}
        var report=new LinkedHashMap<String,Object>();report.put("seed","20261001");report.put("samples",results);report.put("timerMethod","test-only head/return mixins; nested mod callbacks counted once; no profiling classes in release");
        report.put("clockSegmentsObserved",!Boolean.getBoolean("winter.test.skipClockSegments"));
        report.put("clockBoundaryAcceleration",Boolean.getBoolean("winter.test.skipClockSegments")
                ? "visual-only rerun starts at long winter; no clock-boundary observations in this run"
                : "segment starts jump to 30 seconds before the real 60/270 minute boundaries; each then observes 1200 actual running ticks; not a continuous 4.5 hour playtest");
        double off=results.stream().filter(r->r.get("name").toString().startsWith("visual-off")).mapToDouble(r->((Number)r.get("meanFps")).doubleValue()).average().orElseThrow();
        double on=results.stream().filter(r->r.get("name").toString().startsWith("visual-on")).mapToDouble(r->((Number)r.get("meanFps")).doubleValue()).average().orElseThrow();
        report.put("visualOffMeanFps",off);report.put("visualOnMeanFps",on);report.put("visualLossPercent",(off-on)/off*100);
        report.put("inactivityFpsLimit","MINIMIZED; AFK/background time cap disabled in isolated test only");
        require(off>60&&on>60,"uncapped hardware sample must not silently measure the thirty-FPS AFK limit: off="+off+" on="+on);
        try{var path=Path.of(System.getProperty("winter.test.metricsPath","winter-metrics.json"));Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(java.io.IOException e){throw new AssertionError(e);}
        ExtremeWinter.LOGGER.info("TEST E actual parameter time segments and production callback CPU/FPS/memory recording PASSED offFPS={} onFPS={} visualLossPercent={}",off,on,(off-on)/off*100);
    }
    private void clock(ClientGameTestContext context,TestSingleplayerContext game,String name,long ticks,int beforeStage,int afterStage){
        game.getServer().runOnServer(s->{var state=WinterWorldState.get(s.overworld());state.setElapsedTicks(ticks);require(WinterProgression.stage(state.elapsedTicks(),ExtremeWinter.CONFIG)==beforeStage,"actual segment starts at expected stage");});
        var result=sample(context,game,name,1200);
        game.getServer().runOnServer(s->{var now=WinterWorldState.get(s.overworld()).elapsedTicks();require(now-ticks>=1200&&now-ticks<1300,"segment advances by actual running ticks");require(WinterProgression.stage(now,ExtremeWinter.CONFIG)==afterStage,"real parameter boundary crosses after natural elapsed ticks");result.put("elapsedTicksStart",ticks);result.put("elapsedTicksEnd",now);result.put("stageEnd",afterStage);});
    }
    private Map<String,Object> sample(ClientGameTestContext context,TestSingleplayerContext game,String name,int ticks){
        game.getServer().runOnServer(s->WinterWorkProfiler.enable());long started=System.nanoTime();var fps=new ArrayList<Integer>();long[] peakMemory={0};
        for(int i=0;i<ticks/20;i++){context.waitTicks(20);context.runOnClient(c->{fps.add(c.getFps());var runtime=Runtime.getRuntime();peakMemory[0]=Math.max(peakMemory[0],runtime.totalMemory()-runtime.freeMemory());});}
        var times=new java.util.concurrent.atomic.AtomicReference<List<Long>>(List.of());game.getServer().runOnServer(s->{times.set(WinterWorkProfiler.samples());WinterWorkProfiler.disable();});
        long[] sorted=times.get().stream().mapToLong(Long::longValue).sorted().toArray();require(sorted.length>=ticks,"one CPU sample per real server tick");
        var result=new LinkedHashMap<String,Object>();result.put("name",name);result.put("runningTicks",sorted.length);result.put("wallSeconds",(System.nanoTime()-started)/1e9);
        result.put("modCpuMeanMs",Arrays.stream(sorted).average().orElseThrow()/1e6);result.put("modCpuP95Ms",sorted[(int)Math.ceil(sorted.length*.95)-1]/1e6);result.put("modCpuP99Ms",sorted[(int)Math.ceil(sorted.length*.99)-1]/1e6);result.put("modCpuMaxMs",sorted[sorted.length-1]/1e6);
        result.put("meanFps",fps.stream().mapToInt(Integer::intValue).average().orElseThrow());result.put("peakHeapMiB",peakMemory[0]/1048576.0);results.add(result);
        ExtremeWinter.LOGGER.info("TEST E measured segment {}",new GsonBuilder().create().toJson(result));return result;
    }
}
