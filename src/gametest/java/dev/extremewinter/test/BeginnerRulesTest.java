package dev.extremewinter.test;
import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.*;
import dev.extremewinter.environment.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public final class BeginnerRulesTest implements FabricClientGameTest {
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save; long[] elapsed=new long[1]; int[] protection=new int[1];
        double[] respawnWarmth={-1};
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->{
            if(!alive) respawnWarmth[0]=TemperatureData.get(newPlayer);
        });
        try(var game=context.worldBuilder().create()) {
            save=game.getWorldSave();
            context.runOnClient(client->{
                var ctx=Item.TooltipContext.of(client.level);
                for(var item:java.util.List.of(Items.TORCH,Items.FURNACE,Items.LAVA_BUCKET)) {
                    var lines=new ItemStack(item).getTooltipLines(ctx,client.player,TooltipFlag.NORMAL);
                    require(lines.stream().noneMatch(line->line.getString().contains("Durability:")
                            || line.getString().contains("cooling") || line.getString().contains("fall")),
                            "disabled mechanisms hide misleading cooling and durability descriptions for "+item+": "+lines);
                }
            });
            game.getServer().runOnServer(server->{
                var world=server.overworld(); var p=server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL); p.getFoodData().setFoodLevel(16);
                require(p.getInventory().countItem(Items.TORCH)==16,"new kit has sixteen permanent torches");
                require(p.getInventory().countItem(Items.STONE_SHOVEL)==1,"new kit has a shovel");
                require(p.getInventory().countItem(Items.BAKED_POTATO)==4,"new kit has four baked potatoes");
                require(p.getAttachedOrCreate(TemperatureData.PROTECTION)>0,"new player receives protection");
                for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) world.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                p.teleportTo(world,.5,100,.5,Set.of(),0,0,true);
                ExposureTest.time(world,100*24000L+6000);
                require(WinterWorldState.get(world).elapsedTicks()<1200,"old calendar day does not start late winter");
                TemperatureData.set(p,10);
                var cfg=ExtremeWinter.CONFIG;
                var snow=new BlockPos(4,100,0); var env=new WinterEnvironment(cfg);
                for(int i=0;i<16;i++) require(env.trySnow(world,snow),"weather adds layer through sixteen");
                require(!env.trySnow(world,snow),"weather cannot add seventeenth layer");
                world.setBlock(snow.above(2),WinterBlocks.SNOW_DRIFT.defaultBlockState().setValue(SnowLayerBlock.LAYERS,8),3);
                require(!env.trySnow(world,snow) && SnowDriftBlock.layers(world.getBlockState(snow.above(2)))==8,"old excess snow survives");
                var torch=new ItemStack(Items.TORCH); torch.set(HeatItems.EXPOSURE,45);
                require(HeatItems.canPlaceTorch(torch,p)&&!torch.isBarVisible(),"disabled old torch clock permits placement and hides bar");
                HeatItems.recover(torch,cfg); require(!torch.has(HeatItems.EXPOSURE),"old torch clock clears in inventory");
                var pos=new BlockPos(-3,100,0); world.setBlock(pos,Blocks.FURNACE.defaultBlockState(),3);
                var f=(AbstractFurnaceBlockEntity)world.getBlockEntity(pos);
                f.setItem(0,new ItemStack(Items.RAW_IRON,3)); f.setItem(1,new ItemStack(Items.COAL,2));
                f.setAttached(HeatWeathering.BLOCKED,true); HeatItems.writeExposure(f,240);
                require(!HeatWeathering.furnaceBlocked(world,pos,f),"disabled furnace mechanism clears old BLOCKED");
                require(f.getItem(0).getCount()==3&&f.getItem(1).getCount()==2,"old blocked furnace retains inputs and unused fuel");
                AbstractFurnaceBlockEntity.serverTick(world,pos,world.getBlockState(pos),f);
                require(world.getBlockState(pos).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"old furnace can ignite again");
            });
            context.waitTicks(40);
            game.getServer().runOnServer(server->{
                var world=server.overworld(); var p=server.getPlayerList().getPlayers().getFirst(); var cfg=ExtremeWinter.CONFIG;
                require(TemperatureData.get(p)>=70,"preparation keeps Overworld warmth at seventy");
                elapsed[0]=WinterWorldState.get(world).elapsedTicks();
                ExposureTest.time(world,0); require(WinterWorldState.get(world).elapsedTicks()==elapsed[0],"time commands do not alter running clock");
                p.setAttached(TemperatureData.PROTECTION,0);
                p.setAttached(TemperatureData.LOW_TICKS,0);
                for(int sec=0;sec<59;sec++) Hypothermia.apply(p,24,0,cfg);
                require(p.getHealth()==20,"first fifty-nine low seconds cause no damage");
                int low=p.getAttachedOrCreate(TemperatureData.LOW_TICKS);
                Hypothermia.apply(p,26,0,cfg); require(p.getAttachedOrCreate(TemperatureData.LOW_TICKS)==low,"hysteresis holds warning at twenty-six");
                Hypothermia.apply(p,27,0,cfg); require(p.getAttachedOrCreate(TemperatureData.LOW_TICKS)==0,"warming to twenty-seven clears warning");
                p.setAttached(TemperatureData.LOW_TICKS,1200);
                for(int i=0;i<200;i++) { p.invulnerableTime=0; Hypothermia.apply(p,0,0,cfg); }
                require(p.getHealth()==6,"cold stops exactly at six health");
                p.setHealth(4); for(int i=0;i<20;i++) {p.invulnerableTime=0; Hypothermia.apply(p,0,0,cfg);}
                require(p.getHealth()==4,"low health is neither damaged nor healed");
                p.setHealth(20); p.invulnerableTime=0; p.hurtServer(world,p.damageSources().freeze(),1);
                require(p.getAttachedOrCreate(TemperatureData.FREEZE_COOLDOWN)==100,"vanilla freeze starts five-second deduplication");
                p.setAttached(TemperatureData.DAMAGE_TICKS,180); Hypothermia.apply(p,0,0,cfg);
                require(p.getHealth()==19,"mod does not duplicate vanilla freeze");
                p.setHealth(20); p.setAttached(TemperatureData.PROTECTION,120);
                p.setAttached(TemperatureData.LOW_TICKS,400); TemperatureData.set(p,80);
                protection[0]=p.getAttachedOrCreate(TemperatureData.PROTECTION);
                ExposureTest.roof(world,p.blockPosition().above(3),Blocks.GLASS.defaultBlockState());
                p.setGameMode(GameType.SPECTATOR);
            });
        }
        try(var game=save.open()) {
            game.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                require(p.getAttachedOrCreate(TemperatureData.PROTECTION)<=protection[0],"rejoin does not refresh protection");
                require(p.getAttachedOrCreate(TemperatureData.LOW_TICKS)==400,"warning progress persists");
                require(WinterWorldState.get(server.overworld()).elapsedTicks()>=elapsed[0],"world clock persists");
                p.setGameMode(GameType.SURVIVAL); p.setHealth(4); p.getFoodData().setFoodLevel(16);
            });
            // A freshly logged-in vanilla ServerPlayer has its own short damage immunity.
            context.waitTicks(80);
            game.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst(); p.invulnerableTime=0;
                p.hurtServer(p.level(),p.damageSources().drown(),20);
                require(!p.isAlive(),"vanilla drowning still kills below the cold floor");
            });
            context.waitTicks(2);
            context.runOnClient(client->client.player.respawn());
            context.waitTicks(5);
            game.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                require(p.isAlive(),"vanilla death respawns normally");
                require(respawnWarmth[0]==ExtremeWinter.CONFIG.maxTemperature,"death resets warmth before normal cooling resumes: "+respawnWarmth[0]);
                require(p.getAttachedOrCreate(TemperatureData.PROTECTION)>3500,"death receives fresh three-minute cold protection");
                require(p.getInventory().countItem(Items.CAMPFIRE)==0,"death never duplicates starter supplies");
            });
        }
        ExtremeWinter.LOGGER.info("TEST beginner health floor, warning, migration cleanup, preparation and save/rejoin PASSED");
    }
}
