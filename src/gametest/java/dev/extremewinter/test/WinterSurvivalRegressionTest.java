package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Real vanilla danger and native portal travel, with operation receipts earned before actual deaths. */
public final class WinterSurvivalRegressionTest implements FabricClientGameTest {
    private static final BlockPos HOME=new BlockPos(8,100,8),CROP=HOME.north();
    private int harvests;
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static boolean done(ServerPlayer p,String name){return p.getAdvancements().getOrStartProgress(p.level().getServer().getAdvancements().get(WinterGear.id("survival/"+name))).isDone();}
    private static void place(ServerPlayer p,BlockPos support,Direction face,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);require(((BlockItem)stack.getItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(support).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,support,false))).consumesAction(),"actual native placement");}
    public void runTest(ClientGameTestContext context){
        String previous=ExtremeWinter.CONFIG.weatherMode;ExtremeWinter.CONFIG.weatherMode="scheduled";
        try(var game=context.worldBuilder().create()){
            game.getServer().runOnServer(s->{var w=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();s.setDifficulty(Difficulty.HARD,true);
                for(int x=3;x<28;x++)for(int z=3;z<14;z++)w.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                for(int x=5;x<=11;x++)for(int z=5;z<=11;z++)if(x!=8||z!=8)w.setBlock(new BlockPos(x,103,z),Blocks.GLASS.defaultBlockState(),3);
                p.teleportTo(w,8.5,100,8.5,Set.of(),0,0,true);place(p,HOME.above(3).north(),Direction.SOUTH,new ItemStack(Items.GLASS));require(done(p,"roof"),"roof progress actually earned");
                place(p,HOME.east(2).below(),Direction.UP,new ItemStack(HeatingContent.STOVE_ITEM));((HeatingStoveBlockEntity)w.getBlockEntity(HOME.east(2))).setItem(0,new ItemStack(Items.COAL));TemperatureData.set(p,70);
            });context.waitTicks(40);
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var w=p.level();require(done(p,"warming")&&p.getAttachedOrCreate(WinterTasks.STOVE_USED),"actual own stove and warmth progress earned");
                w.setBlock(CROP.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);w.setBlock(CROP,Blocks.POTATOES.defaultBlockState(),3);
                for(int i=0;i<8&&!((CropBlock)Blocks.POTATOES).isMaxAge(w.getBlockState(CROP));i++)BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),w,CROP);
                require(WinterFarming.greenhouse(w,CROP)&&p.gameMode.destroyBlock(CROP),"actual greenhouse harvest");harvests=p.getAttachedOrCreate(WinterTasks.HARVESTS);require(harvests>0&&done(p,"harvest"),"actual produce and advancement recorded");
                WinterWorldState.get(w).setElapsedTicks(90*1200);new WinterWeatherController(ExtremeWinter.CONFIG).update(w);
            });context.waitTicks(240);
            game.getServer().runOnServer(s->{WinterWorldState.get(s.overworld()).setElapsedTicks(93*1200);new WinterWeatherController(ExtremeWinter.CONFIG).update(s.overworld());});context.waitTicks(40);
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(done(p,"storm")&&p.getAttachedOrCreate(WinterTasks.STORM_SEEN),"actual storm interval and ending earned");WinterWorldState.get(s.overworld()).setElapsedTicks(0);});
            // Hunger, fall, AI melee and drowning are actual native processing. Short fixtures are explicit.
            ready(game,6);game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setHealth(1);p.getFoodData().setFoodLevel(0);p.getFoodData().setSaturation(0);});context.waitTicks(100);death(context,game,"hard-mode starvation from one health");
            context.waitTicks(80);ready(game,6);game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),8.5,130,8.5,Set.of(),0,0,true);});context.waitTicks(100);death(context,game,"actual thirty-block fall");
            context.waitTicks(80);ready(game,4);game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var zombie=EntityType.ZOMBIE.create(p.level(),EntitySpawnReason.COMMAND);require(zombie!=null,"native zombie exists");zombie.setPos(9.5,100,8.5);zombie.setBaby(false);zombie.setTarget(p);p.level().addFreshEntity(zombie);});context.waitTicks(120);death(context,game,"native zombie AI attacks below cold floor");
            context.waitTicks(80);ready(game,4);game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var mob:p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(32)))mob.discard();
                for(int x=7;x<=9;x++)for(int z=7;z<=9;z++)for(int y=100;y<=103;y++)p.level().setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),3);p.setAirSupply(0);});context.waitTicks(100);death(context,game,"actual underwater drowning with pre-depleted breath");
            context.waitTicks(80);ready(game,20);game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var w=p.level();for(int x=7;x<=9;x++)for(int z=7;z<=9;z++)for(int y=100;y<=103;y++)w.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                w.setBlock(HOME,Blocks.POWDER_SNOW.defaultBlockState(),3);w.setBlock(HOME.above(),Blocks.POWDER_SNOW.defaultBlockState(),3);p.setTicksFrozen(0);p.setAttached(TemperatureData.PROTECTION,0);
            });context.waitTicks(180);
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.getTicksFrozen()>=140&&p.getHealth()<20,"actual powder snow freezes and damages an unarmored survival player");require(p.getAttachedOrCreate(TemperatureData.FREEZE_COOLDOWN)>0,"native powder-snow hit starts de-duplication window");
                float health=p.getHealth();p.setAttached(TemperatureData.LOW_TICKS,1200);p.setAttached(TemperatureData.DAMAGE_TICKS,180);p.invulnerableTime=0;Hypothermia.apply(p,0,0,ExtremeWinter.CONFIG);require(p.getHealth()==health,"mod does not append damage to real powder-snow hit");wClear(p);
            });
            portals(context,game);
        }finally{ExtremeWinter.CONFIG.weatherMode=previous;}
        ExtremeWinter.LOGGER.info("TEST E native starvation/fall/zombie/drowning/powder snow/death receipts/nether portal round trip and end travel PASSED");
    }
    private static void ready(TestSingleplayerContext game,float health){game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),8.5,100,8.5,Set.of(),0,0,true);p.setHealth(health);p.invulnerableTime=0;p.getFoodData().setFoodLevel(16);p.getFoodData().setSaturation(0);TemperatureData.set(p,100);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});}
    private void death(ClientGameTestContext context,TestSingleplayerContext game,String cause){
        game.getServer().runOnServer(s->require(!s.getPlayerList().getPlayers().getFirst().isAlive(),"native death occurred: "+cause));context.waitTicks(2);context.runOnClient(c->c.player.respawn());context.waitTicks(5);
        game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.isAlive(),"native respawn: "+cause);require(p.getAttachedOrCreate(WinterTasks.HARVESTS)==harvests&&p.getAttachedOrCreate(WinterTasks.STOVE_USED)&&p.getAttachedOrCreate(WinterTasks.STORM_SEEN),"actual earned receipts survive native death: "+cause);
            for(var node:new String[]{"roof","warming","harvest","storm"})require(done(p,node),"native advancement retained after death: "+node);
            require(p.getAttachedOrCreate(TemperatureData.PROTECTION)>3400&&TemperatureData.get(p)>99,"respawn warmth and protection reset");require(p.getInventory().countItem(Items.CAMPFIRE)==0,"death never grants another starter kit");ExtremeWinter.LOGGER.info("TEST E actual native death and retained receipts: {} harvests={}",cause,harvests);
        });
    }
    private static void wClear(ServerPlayer p){p.level().setBlock(HOME,Blocks.AIR.defaultBlockState(),3);p.level().setBlock(HOME.above(),Blocks.AIR.defaultBlockState(),3);p.setTicksFrozen(0);p.setHealth(20);p.setAttached(TemperatureData.PROTECTION,3600);TemperatureData.set(p,50);}
    private static void portals(ClientGameTestContext context,TestSingleplayerContext game){
        long[] before={0};game.getServer().runOnServer(s->{var w=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();for(int x=20;x<=23;x++)for(int y=100;y<=104;y++)w.setBlock(new BlockPos(x,y,8),(x==20||x==23||y==100||y==104?Blocks.OBSIDIAN:Blocks.AIR).defaultBlockState(),3);
            var support=new BlockPos(21,100,8);var flint=new ItemStack(Items.FLINT_AND_STEEL);p.setItemInHand(InteractionHand.MAIN_HAND,flint);p.gameMode.useItemOn(p,w,flint,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false));require(w.getBlockState(support.above()).is(Blocks.NETHER_PORTAL),"native flint-and-steel lights complete obsidian frame");
            p.teleportTo(w,21.5,101,8.5,Set.of(),0,0,true);p.setPortalCooldown(0);before[0]=WinterWorldState.get(w).elapsedTicks();
        });context.waitTicks(110);
        game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.level().dimension().equals(Level.NETHER),"actual native nether portal transfers survival player");require(WinterWorldState.get(p.level()).elapsedTicks()>before[0],"shared winter clock advances across actual portal");
            BlockPos portal=null;for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-6,-4,-6),p.blockPosition().offset(6,4,6)))if(p.level().getBlockState(pos).is(Blocks.NETHER_PORTAL)){portal=pos.immutable();break;}require(portal!=null,"native generated return portal exists");
            p.portalProcess=null;p.setPortalCooldown(0);p.teleportTo(p.level(),portal.getX()+.5,portal.getY(),portal.getZ()+.5,Set.of(),0,0,true);
        });context.waitTicks(110);
        game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.level().dimension().equals(Level.OVERWORLD),"native nether return portal completes round trip");wClear(p);
            var portal=new BlockPos(25,100,8);p.level().setBlock(portal,Blocks.END_PORTAL.defaultBlockState(),3);p.portalProcess=null;p.setPortalCooldown(0);p.teleportTo(p.level(),25.5,100,8.5,Set.of(),0,0,true);
        });context.waitTicks(40);
        game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.level().dimension().equals(Level.END),"native end portal transfers survival player");require(WinterWorldState.get(p.level()).elapsedTicks()>before[0],"end shares running winter clock");
            var portal=p.blockPosition();p.level().setBlock(portal.below(),Blocks.OBSIDIAN.defaultBlockState(),3);p.level().setBlock(portal,Blocks.END_PORTAL.defaultBlockState(),3);p.portalProcess=null;p.setPortalCooldown(0);p.teleportTo(p.level(),portal.getX()+.5,portal.getY(),portal.getZ()+.5,Set.of(),0,0,true);
        });context.waitTicks(40);
        game.getServer().runOnServer(s->require(s.getPlayerList().getPlayers().getFirst().wonGame,"first native End exit opens credits"));
        context.runOnClient(c->{require(c.screen instanceof net.minecraft.client.gui.screens.WinScreen,"native credits screen is present");c.screen.onClose();});
        context.waitTicks(40);
        game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();require(p.level().dimension().equals(Level.OVERWORLD),"native end return portal completes dimension journey");require(WinterWorldState.get(p.level()).elapsedTicks()-before[0]<500,"portal travel never jumps by calendar days");});
    }
}
