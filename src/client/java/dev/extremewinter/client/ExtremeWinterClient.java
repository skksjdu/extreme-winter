package dev.extremewinter.client;

import net.fabricmc.api.ClientModInitializer;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.network.TemperaturePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public final class ExtremeWinterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register(HeatTooltips::append);
        ClientPlayNetworking.registerGlobalReceiver(TemperaturePayload.ID,
                (payload, context) -> TemperatureHud.update(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TemperatureHud.clear());
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(ExtremeWinter.ID, "temperature_hud"), TemperatureHud::render);
    }
}
