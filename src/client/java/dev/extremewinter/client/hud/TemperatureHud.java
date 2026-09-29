package dev.extremewinter.client.hud;

import dev.extremewinter.network.TemperaturePayload;
import dev.extremewinter.ExtremeWinter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class TemperatureHud {
    private static final Identifier EMPTY = Identifier.of(ExtremeWinter.ID, "hud/warmth_empty");
    private static final Identifier HALF = Identifier.of(ExtremeWinter.ID, "hud/warmth_half");
    private static final Identifier FULL = Identifier.of(ExtremeWinter.ID, "hud/warmth_full");
    private static TemperaturePayload current;

    private TemperatureHud() { }

    public static void update(TemperaturePayload value) { current = value; }
    public static TemperaturePayload current() { return current; }
    public static void clear() { current = null; }

    public static int halfIcons(TemperaturePayload value) {
        double fraction = (value.value() - value.minimum()) / (value.maximum() - value.minimum());
        return Math.clamp((int) Math.ceil(fraction * 20), 0, 20);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        var client = MinecraftClient.getInstance();
        if (current == null || client.player == null || client.options.hudHidden
                || client.player.isSpectator() || client.player.isCreative()) return;
        int y = context.getScaledWindowHeight() - 49;
        if (client.player.isSubmergedInWater() || client.player.getAir() < client.player.getMaxAir()) y -= 10;
        if (client.player.getVehicle() instanceof LivingEntity mount) {
            int hearts = Math.min(30, (int) Math.ceil(mount.getMaxHealth() / 2));
            y -= Math.max(0, (int) Math.ceil(hearts / 10.0) - 1) * 10;
        }
        int halves = halfIcons(current);
        for (int i = 0; i < 10; i++) {
            int remaining = halves - i * 2;
            Identifier sprite = remaining >= 2 ? FULL : remaining == 1 ? HALF : EMPTY;
            int x = context.getScaledWindowWidth() / 2 + 91 - 9 - i * 8;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9);
        }
    }
}
