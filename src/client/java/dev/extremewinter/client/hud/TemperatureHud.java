package dev.extremewinter.client.hud;

import dev.extremewinter.network.TemperaturePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import dev.extremewinter.ExtremeWinter;

public final class TemperatureHud {
    private static final Identifier EMPTY = Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "hud/warmth_empty");
    private static final Identifier HALF = Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "hud/warmth_half");
    private static final Identifier FULL = Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "hud/warmth_full");
    private static TemperaturePayload current;

    private TemperatureHud() { }

    public static void update(TemperaturePayload value) { current = value; }
    public static TemperaturePayload current() { return current; }
    public static void clear() { current = null; }

    public static int halfIcons(TemperaturePayload value) {
        double fraction = (value.value() - value.minimum()) / (value.maximum() - value.minimum());
        return Math.clamp((int) Math.ceil(fraction * 20), 0, 20);
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        var client = Minecraft.getInstance();
        if (current == null || client.player == null || client.options.hideGui
                || client.player.isSpectator() || client.player.isCreative()) return;
        int y = context.guiHeight() - 49;
        if (client.player.isUnderWater() || client.player.getAirSupply() < client.player.getMaxAirSupply()) y -= 10;
        if (client.player.getVehicle() instanceof LivingEntity mount) {
            int hearts = Math.min(30, (int) Math.ceil(mount.getMaxHealth() / 2));
            y -= Math.max(0, (int) Math.ceil(hearts / 10.0) - 1) * 10;
        }
        int halves = halfIcons(current);
        for (int i = 0; i < 10; i++) {
            int remaining = halves - i * 2;
            Identifier sprite = remaining >= 2 ? FULL : remaining == 1 ? HALF : EMPTY;
            int x = context.guiWidth() / 2 + 91 - 9 - i * 8;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9);
        }
    }
}
