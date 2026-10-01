package dev.extremewinter.test;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.*;
import dev.extremewinter.environment.WinterWeatherModel.Weather;
import dev.extremewinter.temperature.*;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.network.WinterStatusSync;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import java.util.Set;

public final class WinterWeatherTest implements FabricClientGameTest {
    private static void require(boolean b,String m) {if(!b)throw new AssertionError(m);}
    public void runTest(ClientGameTestContext context) {
        var cfg=ExtremeWinter.CONFIG;
        String old=cfg.weatherMode;int radius=cfg.simulationRadiusChunks,samples=cfg.samplesPerPass;
        cfg.weatherMode="scheduled";cfg.simulationRadiusChunks=0;cfg.samplesPerPass=64;
        var controller=new WinterWeatherController(cfg);long[] next=new long[1],duration=new long[1],clock=new long[1];
        long[] beforeSleep=new long[1];
        TestWorldSave save;
        try {
            try(var game=context.worldBuilder().create()) {
                save=game.getWorldSave();
                game.getServer().runOnServer(server->{
                    var world=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                    p.setGameMode(GameType.SURVIVAL);p.teleportTo(world,8.5,110,8.5,Set.of(),0,0,true);
                    for(int x=0;x<16;x++)for(int z=0;z<16;z++)world.setBlock(new BlockPos(x,109,z),Blocks.STONE.defaultBlockState(),3);
                    for(int x=0;x<4;x++)for(int z=0;z<16;z++)world.setBlock(new BlockPos(x,109,z),Blocks.WATER.defaultBlockState(),3);
                    world.setBlock(new BlockPos(12,109,10),Blocks.WATER.defaultBlockState(),3);
                    ExposureTest.roof(world,new BlockPos(12,113,10),Blocks.GLASS.defaultBlockState());
                    var s=WinterWorldState.get(world);s.setElapsedTicks(0);controller.update(world);
                    s.put("windowKind",1);s.put("windowEndTick",20*1200);controller.update(world);controller.applyVanilla(world);
                    require(WinterWeatherController.current(world)==Weather.SNOW,"preparation has ordinary snow, never blizzard");
                    require(WinterWeatherController.snowFactor(world,cfg)==.25&&WinterWeatherController.freezingFactor(world,cfg)==0,"preparation extra snow quarter rate and extra freezing off");
                    s.setElapsedTicks(85*1200);controller.update(world);controller.applyVanilla(world);
                    require(WinterWeatherController.current(world)==Weather.WARNING,"five-minute first warning");
                    s.setElapsedTicks(90*1200);controller.update(world);controller.applyVanilla(world);
                    require(WinterWeatherController.current(world)==Weather.BLIZZARD,"ninety-minute first blizzard");
                    require(WinterWeatherController.snowFactor(world,cfg)==2,"blizzard doubles passes without expanding candidate budget");
                    WinterStatusSync.send(p,cfg);
                });
                context.waitTicks(240);
                game.getClientLevel().waitForChunksRender();
                context.runOnClient(client->{
                    TemperatureHud.updateWinter(new dev.extremewinter.network.WinterStatusPayload(1,3,0,180,90*1200));
                    int count=dev.extremewinter.client.SnowstormEffects.emit(client.level,new BlockPos(8,110,8),net.minecraft.util.RandomSource.create(42));
                    require(count>0&&count<=4,"bounded native slanted snow particles enter the client engine");
                    ExtremeWinter.CONFIG.blizzardParticles=false;
                    require(dev.extremewinter.client.SnowstormEffects.emit(client.level,new BlockPos(8,110,8),net.minecraft.util.RandomSource.create(42))==0,"visual switch suppresses extra particles");
                    ExtremeWinter.CONFIG.blizzardParticles=true;
                });
                game.getServer().runOnServer(server->{
                    var world=server.overworld();var s=WinterWorldState.get(world);
                    require(layers(world)>0,"real scheduled blizzard forms actual snow blocks");
                    int ice=0;
                    for(int x=0;x<4;x++)for(int z=0;z<16;z++)if(world.getBlockState(new BlockPos(x,109,z)).is(Blocks.ICE))ice++;
                    require(ice>0,"real scheduled blizzard freezes exposed source water");
                    require(world.getBlockState(new BlockPos(12,109,10)).is(Blocks.WATER),"glass roof protects source water during a real blizzard");
                    ExtremeWinter.LOGGER.info("TEST scheduled blizzard observation: actual snowLayers={}, sourceIce={} (test-only radius=0 samples=64)",layers(world),ice);
                    for(int x=0;x<16;x++)for(int z=0;z<16;z++) require(column(world,x,z)<=16,"weather never exceeds sixteen layers");
                    s.setElapsedTicks(93*1200);controller.update(world);controller.applyVanilla(world);
                    require(WinterWeatherController.current(world)==Weather.EBB,"three-minute first blizzard ebbs");
                    s.setElapsedTicks(95*1200);controller.update(world);controller.applyVanilla(world);
                    require(WinterWeatherController.current(world)==Weather.RELIEF,"two-minute ebb opens a relief window");
                    require(WinterWeatherController.snowFactor(world,cfg)==0,"relief stops extra snow");
                    next[0]=s.value("nextBlizzardTick",0);duration[0]=s.value("blizzardDurationTicks",0);
                    require(next[0]>=138*1200&&next[0]<=168*1200,"next saved plan is forty-five to seventy-five minutes from first end");
                    ExposureTest.time(world,500*24000L+13000);
                    require(s.value("nextBlizzardTick",0)==next[0],"calendar changes cannot reroll planned weather");
                    world.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,true,server);
                    world.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                    var foot=new BlockPos(8,110,9);
                    var bed=Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING,net.minecraft.core.Direction.NORTH);
                    world.setBlock(foot,bed.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT),3);
                    world.setBlock(foot.north(),bed.setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),3);
                });
                context.waitTicks(2);
                game.getServer().runOnServer(server->{
                    var world=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                    var result=p.startSleepInBed(new BlockPos(8,110,9));
                    require(p.isSleeping(),"real player enters a vanilla bed: "+result+" pos="+p.position()+" clock="+world.getOverworldClockTime());
                    beforeSleep[0]=WinterWorldState.get(world).elapsedTicks();
                });
                context.waitTicks(120);
                game.getServer().runOnServer(server->{
                    var world=server.overworld();var s=WinterWorldState.get(world);var p=server.getPlayerList().getPlayers().getFirst();
                    require(!p.isSleeping()&&world.getOverworldClockTime()%24000<1000,"vanilla sleep advances calendar to morning");
                    require(s.elapsedTicks()-beforeSleep[0]<400,"sleep advances winter only by ticks actually run");
                    require(s.value("nextBlizzardTick",0)==next[0],"real sleep does not clear or redraw blizzard plan");
                    p.setGameMode(GameType.SPECTATOR);
                    clock[0]=s.elapsedTicks();
                });
                // Native rain strength fades over ~100 ticks after raining=false.
                context.waitTicks(120);
                game.getServer().runOnServer(server->{
                    require(WinterWorldState.get(server.overworld()).elapsedTicks()==clock[0],"no surviving non-spectator pauses shared clock");
                    require(!server.overworld().isRaining(),"single controller keeps relief clear despite persistentWeather=true");
                });
                context.runOnClient(client->require(TemperatureHud.winter()!=null,"independent winter status reaches client"));
                game.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
                context.runOnClient(client->client.pauseGame(false));
                context.waitTicks(2);
                context.runOnClient(client->require(client.isPaused(),"real singleplayer pause menu is active"));
                game.getServer().runOnServer(server->clock[0]=WinterWorldState.get(server.overworld()).elapsedTicks());
                context.waitTicks(40);
                game.getServer().runOnServer(server->require(WinterWorldState.get(server.overworld()).elapsedTicks()==clock[0],"real pause menu does not advance winter"));
                context.setScreen(()->null);
                context.waitTicks(2);
                game.getServer().runOnServer(server->{
                    server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
                    clock[0]=WinterWorldState.get(server.overworld()).elapsedTicks();
                });
            }
            try(var game=save.open()) {
                game.getServer().runOnServer(server->{
                    var w=server.overworld();var s=WinterWorldState.get(w);
                    require(s.value("nextBlizzardTick",0)==next[0]&&s.value("blizzardDurationTicks",0)==duration[0],"save/rejoin does not redraw plan or duration");
                    require(s.elapsedTicks()==clock[0],"save/rejoin has no offline catch-up");
                    var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);
                    require(WinterWorldState.get(nether)==s,"dimensions share exactly one state");
                    cfg.weatherMode="vanilla";ExposureTest.weather(w,6000,0,false,false);controller.tick(server);
                    require(!w.getWeatherData().isRaining(),"vanilla mode leaves manual clear weather intact");
                    cfg.weatherMode="legacy";controller.onLoad(w);
                    require(w.getWeatherData().isRaining(),"explicit legacy mode preserves persistent precipitation");
                });
            }
        } finally {cfg.weatherMode=old;cfg.simulationRadiusChunks=radius;cfg.samplesPerPass=samples;}
        ExtremeWinter.LOGGER.info("TEST saved weather schedule, first event boundaries, real snow, relief, cross-dimension and rejoin PASSED");
    }
    private static int column(net.minecraft.server.level.ServerLevel w,int x,int z) {int n=0;for(int y=110;y<114;y++)n+=SnowDriftBlock.layers(w.getBlockState(new BlockPos(x,y,z)));return n;}
    private static int layers(net.minecraft.server.level.ServerLevel w) {int n=0;for(int x=0;x<16;x++)for(int z=0;z<16;z++)n+=column(w,x,z);return n;}
}
