package dev.extremewinter.network;

import dev.extremewinter.config.WinterConfig;
import dev.extremewinter.temperature.TemperatureData;
import dev.extremewinter.temperature.TemperatureModel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class TemperatureSync {
    private TemperatureSync() { }

    public static void register(WinterConfig config) {
        PayloadTypeRegistry.clientboundPlay().register(TemperaturePayload.ID, TemperaturePayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, config));
    }

    public static void send(ServerPlayer player, WinterConfig config) {
        send(player, config, 0, 0);
    }

    public static void send(ServerPlayer player, WinterConfig config, int trend, int reasons) {
        if (!ServerPlayNetworking.canSend(player, TemperaturePayload.ID)) return;
        double value = TemperatureModel.clamp(TemperatureData.get(player), config);
        ServerPlayNetworking.send(player, new TemperaturePayload(value, config.minTemperature,
                config.maxTemperature, TemperatureModel.stage(value, config), trend, reasons,
                value < config.damageThreshold ? Math.max(0, config.damageWarningSeconds - player.getAttachedOrCreate(TemperatureData.LOW_TICKS) / 20) : -1,
                (player.getAttachedOrCreate(TemperatureData.PROTECTION) + 19) / 20));
    }
}
