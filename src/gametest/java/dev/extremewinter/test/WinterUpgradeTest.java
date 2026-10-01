package dev.extremewinter.test;

import java.nio.file.*;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.survival.*;
import dev.extremewinter.temperature.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

/** Opens a copied authentic older save with the current production JAR; source fixture stays untouched. */
public final class WinterUpgradeTest implements FabricClientGameTest {
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    public void runTest(ClientGameTestContext context){
        var source=Path.of(System.getProperty("winter.test.legacyWorld"));
        require(Files.isRegularFile(source.resolve("winter-legacy-evidence.json")),"genuine old-JAR fixture evidence exists");
        double[] joinedWarmth={Double.NaN}; boolean[] capture={false};
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,s)->{if(capture[0])joinedWarmth[0]=TemperatureData.get(handler.player);});
        TestWorldSave save;
        try(var game=context.worldBuilder().create()){save=game.getWorldSave();}
        var target=save.getSaveDirectory();
        try{
            // Only this newly created test save is changed. Remove its newer clock before copying old native data.
            try(var files=Files.walk(target)){for(var path:files.filter(p->p.getFileName().toString().equals("extreme_winter_world.dat")).toList())Files.delete(path);}
            try(var files=Files.walk(source)){for(var path:files.toList()){
                if(path.getFileName().toString().equals("session.lock"))continue;
                var destination=target.resolve(source.relativize(path)).normalize();require(destination.startsWith(target.normalize()),"upgrade copy stays within exact test save");
                if(Files.isDirectory(path))Files.createDirectories(destination);else Files.copy(path,destination,StandardCopyOption.REPLACE_EXISTING);
            }}
        }catch(java.io.IOException e){throw new AssertionError(e);}
        capture[0]=true;
        try(var game=save.open()){
            game.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var w=p.level();long elapsed=WinterWorldState.get(w).elapsedTicks();
                require(w.getOverworldClockTime()>=100*24000L,"native old 100-day calendar is preserved");
                require(elapsed<200&&WinterProgression.stage(elapsed,ExtremeWinter.CONFIG)==0,"genuine old save begins new preparation instead of jumping to long winter");
                require(p.getInventory().countItem(Items.COAL)==5&&p.getInventory().countItem(Items.APPLE)==3,"old native inventory survives upgrade");
                require(p.getInventory().countItem(Items.CAMPFIRE)==0,"old starter receipt prevents another kit");
                require(p.getInventory().countItem(WinterItems.SURVIVAL_MANUAL)==1,"new manual is delivered once to genuine old player");
                require(Math.abs(joinedWarmth[0]-42)<2,"old warmth attachment is preserved at join before preparation recovery");
                var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)w.getBlockEntity(new BlockPos(10,100,8));require(chest.getItem(0).is(Items.IRON_INGOT)&&chest.getItem(0).getCount()==7,"old placed native chest and content survive");
                StarterSupplies.onJoin(p);require(p.getInventory().countItem(WinterItems.SURVIVAL_MANUAL)==1,"upgrade guide receipt prevents duplicates");
                ExtremeWinter.LOGGER.info("TEST E genuine 26.0.2 world upgrade calendar={} winterTicks={} joinWarmth={} currentWarmth={} oldInventory and chest retained",w.getOverworldClockTime(),elapsed,joinedWarmth[0],TemperatureData.get(p));
            });
        }
        ExtremeWinter.LOGGER.info("TEST E genuine old-JAR save upgrade/preparation/attachments/inventory/starter and manual receipts PASSED");
    }
}
