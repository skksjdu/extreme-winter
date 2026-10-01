package dev.extremewinter.client;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.Level;

/** Gentle screen haze and pale edge frost; uses the native GUI layer without changing world fog or Iris. */
public final class BlizzardOverlay {
    private static float haze, frost;
    private BlizzardOverlay() { }
    public static float haze() { return ExtremeWinter.CONFIG.blizzardHaze ? haze : 0; }
    public static float frost() { return ExtremeWinter.CONFIG.blizzardFrost ? frost : 0; }
    public static void clear() { haze = frost = 0; }
    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) { clear(); return; }
        if (client.isPaused()) return;
        var warmth = TemperatureHud.current(); var weather = TemperatureHud.winter();
        boolean visible = client.level.dimension().equals(Level.OVERWORLD) && !client.player.isSpectator()
                && !client.player.isCreative() && warmth != null;
        float targetHaze = visible && weather != null && weather.weather() == 3
                ? ((warmth.reasons() & 1) == 0 ? .045f : .012f) : 0;
        float targetFrost = visible ? (float) Math.clamp((40 - warmth.value()) / 40, 0, 1) * .18f : 0;
        haze = approach(haze, targetHaze, .003f); frost = approach(frost, targetFrost, .006f);
    }
    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }
    private static int color(float alpha) { return Math.clamp((int) (alpha * 255), 0, 255) << 24 | 0xe2edf0; }
    public static void render(GuiGraphicsExtractor gui, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || client.player.isSpectator() || client.player.isCreative()) return;
        int width = gui.guiWidth(), height = gui.guiHeight();
        if (haze() > .001) gui.fillGradient(0, 0, width, height, color(haze()), color(haze() * .5f));
        if (frost() <= .001) return;
        // A fixed eight-step perimeter leaves the centre and all HUD text unobscured.
        int edge = Math.min(20, Math.min(width, height) / 10);
        for (int i = 0; i < 8; i++) {
            int a = i * edge / 8, b = (i + 1) * edge / 8;
            int tint = color(frost() * (8 - i) / 8);
            gui.fill(a, a, b, height - a, tint); gui.fill(width - b, a, width - a, height - a, tint);
            gui.fill(b, a, width - b, b, tint); gui.fill(b, height - b, width - b, height - a, tint);
        }
    }
}
