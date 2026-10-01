package dev.extremewinter.network;
import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.temperature.WinterProgression;
import dev.extremewinter.temperature.WinterWorldState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class WinterStatusSync {
    private WinterStatusSync() { }
    public static void register(WinterConfig config) {
        PayloadTypeRegistry.clientboundPlay().register(WinterStatusPayload.ID,WinterStatusPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->send(handler.player,config));
    }
    public static WinterStatusPayload status(ServerPlayer p,WinterConfig c) {
        var world=p.level();var s=WinterWorldState.get(world);long now=s.elapsedTicks();
        boolean scheduled=c.weatherMode.equals("scheduled") && world.dimension().equals(Level.OVERWORLD);
        return new WinterStatusPayload(WinterProgression.stage(now,c),scheduled?WinterWeatherController.current(world).ordinal():-1,
                scheduled?seconds(s.value("nextBlizzardTick",90*1200)-now):-1,
                scheduled?seconds(s.value("weatherEndTick",0)-now):-1,now);
    }
    private static int seconds(long ticks) {return (int)Math.clamp((ticks+19)/20,0,Integer.MAX_VALUE);}
    public static void send(ServerPlayer p,WinterConfig c) {
        if(ServerPlayNetworking.canSend(p,WinterStatusPayload.ID)) ServerPlayNetworking.send(p,status(p,c));
    }
}
