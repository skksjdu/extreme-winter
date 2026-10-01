package dev.extremewinter.test;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.BlizzardOverlay;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.network.*;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Native rendering of original assets, independently disabled effects and mounted/underwater HUD. */
public final class WinterVisualTest implements FabricClientGameTest {
    private static void require(boolean b, String m) { if (!b) throw new AssertionError(m); }
    public void runTest(ClientGameTestContext context) {
        var cfg = ExtremeWinter.CONFIG; boolean oldHaze=cfg.blizzardHaze, oldFrost=cfg.blizzardFrost, oldParticles=cfg.blizzardParticles;
        String oldWeather=cfg.weatherMode; cfg.weatherMode="scheduled";
        try (var game = context.worldBuilder().create()) {
            game.getServer().runOnServer(s -> {
                var w=s.overworld(); var p=s.getPlayerList().getPlayers().getFirst(); p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                for(int x=1;x<16;x++) for(int z=1;z<16;z++) w.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                p.teleportTo(w,8.5,100,8.5,Set.of(),0,0,true); TemperatureData.set(p,10);
                ItemStack[] items={new ItemStack(WinterItems.THERMAL_LINING),new ItemStack(WinterItems.HOT_WATER_BOTTLE),new ItemStack(WinterItems.WARMING_STEW),
                        new ItemStack(WinterItems.SURVIVAL_MANUAL),new ItemStack(WinterItems.WARMTH_METER),new ItemStack(HeatingContent.STOVE_ITEM),new ItemStack(WinterItems.WEATHER_INSTRUMENT_ITEM)};
                for(int i=0;i<items.length;i++)p.getInventory().setItem(i,items[i]);
                WinterWorldState.get(w).setElapsedTicks(90*1200); var weather=new WinterWeatherController(cfg); weather.update(w); weather.applyVanilla(w);
                TemperatureSync.send(p,cfg,0,0); WinterStatusSync.send(p,cfg); p.inventoryMenu.broadcastChanges();
            });
            context.waitTicks(40); game.getClientLevel().waitForChunksRender();
            context.runOnClient(c -> {
                c.gui.getChat().clearMessages(false);
                require(BlizzardOverlay.haze()>0&&BlizzardOverlay.frost()>0,"real blizzard and low warmth render two independent overlays");
                var status=TemperatureHud.winter(); long before=WinterGear.clientClock(); int seconds=TemperatureHud.eventSeconds();
                WinterGear.updateClientClock(before+20); require(TemperatureHud.eventSeconds()==Math.max(0,seconds-1),"countdown interpolates a second between server packets"); WinterGear.updateClientClock(before);
                cfg.blizzardHaze=false; require(BlizzardOverlay.haze()==0&&BlizzardOverlay.frost()>0,"haze switch preserves frost");
                cfg.blizzardFrost=false; require(BlizzardOverlay.frost()==0&&TemperatureHud.winter().equals(status),"graphics switches leave server danger status intact");
            });
            context.waitTicks(2);context.takeScreenshot("beginner-E-effects-off");
            context.runOnClient(c->{cfg.blizzardHaze=true;cfg.blizzardFrost=true;});context.waitTicks(2);context.takeScreenshot("beginner-E-effects-on");
            context.runOnClient(c->{cfg.blizzardHaze=false;cfg.blizzardFrost=false;cfg.blizzardParticles=false;});
            game.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var w=p.level();require(WinterWeatherController.current(w)==dev.extremewinter.environment.WinterWeatherModel.Weather.BLIZZARD,"all visual switches retain real server blizzard");
                var horse=new Horse(EntityType.HORSE,w);horse.setPos(8.5,100,8.5);horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);horse.setHealth(60);horse.setTamed(true);w.addFreshEntity(horse);
                require(p.startRiding(horse,true,true),"native player mounts a living sixty-health horse");
            });
            context.waitTicks(5);context.takeScreenshot("beginner-E-mounted-HUD");
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();
                for (var horse : p.level().getEntitiesOfClass(Horse.class, p.getBoundingBox().inflate(8))) horse.discard();
                for(int x=7;x<=9;x++)for(int z=7;z<=9;z++)for(int y=100;y<=103;y++)p.level().setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),3);
                p.teleportTo(p.level(),8.5,100,8.5,Set.of(),0,0,true);});
            context.waitTicks(70);context.runOnClient(c->require(c.player.isUnderWater(),"native player eye is underwater"));context.takeScreenshot("beginner-E-underwater-HUD");
            context.runOnClient(c->c.options.hideGui=true);context.waitTicks(2);context.takeScreenshot("beginner-E-hidden-HUD");context.runOnClient(c->c.options.hideGui=false);
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),12.5,100,8.5,Set.of(),0,0,true);});
            context.runOnClient(c->c.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.player)));
            context.waitTicks(2);context.takeScreenshot("beginner-E-original-item-assets");context.setScreen(()->null);
        } finally { cfg.blizzardHaze=oldHaze;cfg.blizzardFrost=oldFrost;cfg.blizzardParticles=oldParticles;cfg.weatherMode=oldWeather;context.runOnClient(c->c.options.hideGui=false); }
        ExtremeWinter.LOGGER.info("TEST E original pixel assets/haze and frost switches/server independence/countdown/mounted underwater hidden HUD PASSED");
    }
}
