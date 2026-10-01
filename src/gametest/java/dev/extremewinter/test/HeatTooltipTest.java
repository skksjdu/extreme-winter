package dev.extremewinter.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

public final class HeatTooltipTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        boolean oldtorchWeathering=ExtremeWinter.CONFIG.torchWeathering; ExtremeWinter.CONFIG.torchWeathering=true;
        boolean oldfurnaceWeathering=ExtremeWinter.CONFIG.furnaceWeathering; ExtremeWinter.CONFIG.furnaceWeathering=true;
        boolean oldlavaCooling=ExtremeWinter.CONFIG.lavaCooling; ExtremeWinter.CONFIG.lavaCooling=true;
        try {
        String profile = System.getProperty("winter.test.profile", "A");
        try (var game = context.worldBuilder().create()) {
            game.getClientLevel().waitForChunksRender();
            language(context, "en_us");
            context.runOnClient(client -> {
                var tooltipContext = Item.TooltipContext.of(client.level);
                for (var item : new Item[]{Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.FURNACE,
                        Items.BLAST_FURNACE, Items.SMOKER, Items.TORCH, Items.SOUL_TORCH}) {
                    var stack = new ItemStack(item);
                    require(!stack.isBarVisible(), "every unused source hides its bar");
                    var full = text(stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.NORMAL));
                    require(full.stream().anyMatch(line -> line.contains("Durability: 100%")),
                            "hover explains full durability even while the bar is hidden");
                    stack.set(HeatItems.EXPOSURE, HeatItems.limit(item, ExtremeWinter.CONFIG) / 2);
                    var used = text(stack.getTooltipLines(tooltipContext, client.player, TooltipFlag.NORMAL));
                    require(stack.isBarVisible() && used.stream().anyMatch(line -> line.contains("Durability:"))
                            && used.stream().noneMatch(line -> line.contains("Durability: 100%")),
                            "used item shows its bar and changing remaining duration");
                    if (HeatItems.isTorch(stack)) require(used.stream().anyMatch(line -> line.contains("Ready to place in:")),
                            "cooling torch tooltip reports remaining placement cooldown");
                }
                var lava = text(new ItemStack(Items.LAVA_BUCKET).getTooltipLines(tooltipContext, client.player, TooltipFlag.NORMAL));
                require(lava.stream().anyMatch(line -> line.contains("Source cooling time:")), "lava bucket explains its world cooling rule");
                var sword = new ItemStack(Items.STONE_SWORD);
                sword.setDamageValue(80);
                require(sword.isBarVisible() && text(sword.getTooltipLines(tooltipContext, client.player, TooltipFlag.NORMAL))
                        .stream().noneMatch(line -> line.contains("Heat source")), "ordinary items keep their own tooltip and durability");
            });
            game.getServer().runOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                inventory.clearContent();
                var items = new Item[]{Items.CAMPFIRE, Items.CAMPFIRE, Items.FURNACE, Items.FURNACE,
                        Items.TORCH, Items.TORCH, Items.SOUL_TORCH, Items.SOUL_TORCH, Items.STONE_SWORD};
                for (int slot = 0; slot < items.length; slot++) {
                    var stack = new ItemStack(items[slot]);
                    if (slot < 8 && slot % 2 == 1) stack.set(HeatItems.EXPOSURE, HeatItems.limit(items[slot], ExtremeWinter.CONFIG) / 2);
                    if (slot == 8) stack.setDamageValue(80);
                    inventory.setItem(slot, stack);
                }
                inventory.setChanged();
            });
            context.waitTicks(2);
            context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.getInput().setCursorPos(10, 10);
            context.takeScreenshot("heat-bars-26.0.1-" + profile);
            hover(context, 1);
            context.takeScreenshot("campfire-tooltip-en-26.0.1-" + profile);
            language(context, "zh_cn");
            context.runOnClient(client -> require(text(new ItemStack(Items.CAMPFIRE).getTooltipLines(
                    Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)).stream()
                    .anyMatch(line -> line.contains("蹲下空手右键")), "Chinese pickup instructions are translated"));
            hover(context, 1);
            context.takeScreenshot("campfire-tooltip-zh-26.0.1-" + profile);
            hover(context, 3);
            context.takeScreenshot("furnace-tooltip-zh-26.0.1-" + profile);
            game.getServer().runOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                var cold = new ItemStack(Items.TORCH);
                cold.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                inventory.setItem(5, cold);
                inventory.setChanged();
            });
            context.waitTicks(2);
            hover(context, 5);
            context.takeScreenshot("torch-tooltip-zh-26.0.1-" + profile);
            context.setScreen(() -> null);
            language(context, "en_us");
        }
        } finally {
            ExtremeWinter.CONFIG.torchWeathering=oldtorchWeathering;
            ExtremeWinter.CONFIG.furnaceWeathering=oldfurnaceWeathering;
            ExtremeWinter.CONFIG.lavaCooling=oldlavaCooling;
        }
        ExtremeWinter.LOGGER.info("TEST used-only bars, heat tooltips, Chinese/English hover and ordinary item preservation PASSED");
    }

    private static List<String> text(List<Component> lines) { return lines.stream().map(Component::getString).toList(); }

    private static void hover(ClientGameTestContext context, int slot) {
        double[] position = context.computeOnClient(client -> {
            var window = client.getWindow();
            double scale = window.getGuiScale();
            return new double[]{((window.getGuiScaledWidth() - 176) / 2 + 8 + slot * 18 + 8) * scale,
                    ((window.getGuiScaledHeight() - 166) / 2 + 142 + 8) * scale};
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTick();
    }

    private static void language(ClientGameTestContext context, String language) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> {
            client.options.languageCode = language;
            client.getLanguageManager().setSelected(language);
            return client.reloadResourcePacks();
        });
        context.waitFor(client -> reload.isDone(), 400);
        reload.join();
        context.waitFor(client -> client.getOverlay() == null, 400);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
