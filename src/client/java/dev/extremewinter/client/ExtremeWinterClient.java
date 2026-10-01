package dev.extremewinter.client;

import net.fabricmc.api.ClientModInitializer;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import dev.extremewinter.network.TemperaturePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class ExtremeWinterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register(HeatTooltips::append);
        ItemTooltipCallback.EVENT.register(GearTooltips::append);
        net.minecraft.client.gui.screens.MenuScreens.register(dev.extremewinter.survival.HeatingContent.STOVE_MENU, HeatingStoveScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) dev.extremewinter.survival.WinterGear.tickClientClock();
        });
        ClientTickEvents.END_CLIENT_TICK.register(BlizzardOverlay::tick);
        ClientTickEvents.END_LEVEL_TICK.register(CanopySnowflakes::tick);
        ClientTickEvents.END_LEVEL_TICK.register(SnowstormEffects::tick);
        ClientPlayNetworking.registerGlobalReceiver(TemperaturePayload.ID,
                (payload, context) -> TemperatureHud.update(payload));
        ClientPlayNetworking.registerGlobalReceiver(dev.extremewinter.network.WinterStatusPayload.ID,
                (payload, context) -> { TemperatureHud.updateWinter(payload);
                    dev.extremewinter.survival.WinterGear.updateClientClock(payload.elapsedTicks()); });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { TemperatureHud.clear(); BlizzardOverlay.clear(); });
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "winter_overlay"), BlizzardOverlay::render);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "temperature_hud"), TemperatureHud::render);
    }
}
