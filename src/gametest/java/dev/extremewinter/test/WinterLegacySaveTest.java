package dev.extremewinter.test;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import dev.extremewinter.temperature.TemperatureData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Creates a genuine 26.0.2 save using its retained official JAR. No newer mod classes are referenced. */
public final class WinterLegacySaveTest implements FabricClientGameTest {
    private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
    public void runTest(ClientGameTestContext context){
        String version=FabricLoader.getInstance().getModContainer("extreme_winter").orElseThrow().getMetadata().getVersion().getFriendlyString();
        require(version.equals("26.0.2"),"legacy fixture is really running the retained 26.0.2 JAR");
        Path[] save={null};ServerPlayer[] owner={null};
        try(var game=context.worldBuilder().create()){
            save[0]=game.getWorldSave().getSaveDirectory();
            game.getServer().runOnServer(s->{var w=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();owner[0]=p;p.setGameMode(GameType.SURVIVAL);
                require(p.getInventory().countItem(Items.CAMPFIRE)==1,"legacy starter is genuinely granted once");
                p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.COAL,5));p.getInventory().setItem(1,new ItemStack(Items.APPLE,3));
                for(int x=5;x<=11;x++)for(int z=5;z<=11;z++){w.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);w.setBlock(new BlockPos(x,103,z),Blocks.GLASS.defaultBlockState(),3);}
                p.teleportTo(w,8.5,100,8.5,Set.of(),0,0,true);ExposureTest.time(w,100*24000L);TemperatureData.set(p,42);
                var chestPos=new BlockPos(10,100,8);w.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
                ((net.minecraft.world.level.block.entity.ChestBlockEntity)w.getBlockEntity(chestPos)).setItem(0,new ItemStack(Items.IRON_INGOT,7));
            });
        }
        try{
            var target=Path.of(System.getProperty("winter.test.legacyWorld"));require(!Files.exists(target),"existing legacy evidence is preserved; use a new attempt number");Files.createDirectories(target);
            try(var paths=Files.walk(save[0])){for(var source:paths.toList()){
                if(source.getFileName().toString().equals("session.lock"))continue;
                var destination=target.resolve(save[0].relativize(source)).normalize();require(destination.startsWith(target.normalize()),"fixture copy stays within exact destination");
                if(Files.isDirectory(source))Files.createDirectories(destination);else Files.copy(source,destination);
            }}
            var evidence=Map.of("modVersion",version,"temperature",TemperatureData.get(owner[0]),"calendar",100*24000L,"coal",5,"apples",3,"chestIron",7);
            Files.writeString(target.resolve("winter-legacy-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence)+"\n");
            require(Files.exists(target.resolve("level.dat")),"native legacy world is saved to durable isolated fixture");
            System.out.println("TEST E genuine 26.0.2 legacy world created at "+target+" PASSED");
        }catch(java.io.IOException e){throw new AssertionError(e);}
    }
}
