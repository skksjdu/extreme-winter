package dev.extremewinter.client;

import dev.extremewinter.survival.HeatingStoveMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Small vanilla-style panel: one fuel slot, a running duration and the player inventory. */
public final class HeatingStoveScreen extends AbstractContainerScreen<HeatingStoveMenu> {
    public HeatingStoveScreen(HeatingStoveMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff373737);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xffeeeeee);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xffc6c6c6);
        for (var slot : menu.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xff373737);
            g.fill(sx, sy, sx + 17, sy + 17, 0xffffffff);
            g.fill(sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable("container.extreme_winter.stove_time", (menu.remainingTicks() + 19) / 20), 8, 61, 0xff404040, false);
        g.text(font, Component.translatable("container.extreme_winter.stove_fuel"), 8, 24, 0xff404040, false);
    }
}
