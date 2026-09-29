package dev.extremewinter.client.hud;

import dev.extremewinter.network.TemperaturePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class TemperatureHud {
    private static final int[] COLORS = {0xFFE4F1F5, 0xFFAADBF0, 0xFF79BEDF, 0xFFFFB26B, 0xFFFF7777};
    private static TemperaturePayload current;

    private TemperatureHud() { }

    public static void update(TemperaturePayload value) { current = value; }
    public static TemperaturePayload current() { return current; }
    public static void clear() { current = null; }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        var client = MinecraftClient.getInstance();
        if (current == null || client.player == null || client.options.hudHidden
                || client.player.isSpectator() || client.player.isCreative()) return;
        int stage = Math.clamp(current.stage(), 0, 4);
        Text label = Text.translatable("hud.extreme_winter.temperature", (int) Math.ceil(current.value()),
                (int) current.maximum(), Text.translatable("hud.extreme_winter.stage." + stage));
        int width = client.textRenderer.getWidth(label);
        context.fill(5, 5, width + 13, 21, 0xA0202930);
        context.drawTextWithShadow(client.textRenderer, label, 9, 9, COLORS[stage]);
    }
}
