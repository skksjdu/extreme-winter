package dev.extremewinter.test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.HeatItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public final class HeatTooltipTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        String profile = System.getProperty("winter.test.profile", "A");
        try (var game = context.worldBuilder().create()) {
            game.getClientWorld().waitForChunksRender();
            language(context, "en_us");
            context.runOnClient(client -> {
                var tooltipContext = Item.TooltipContext.create(client.world);
                for (var item : new Item[]{Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.FURNACE,
                        Items.BLAST_FURNACE, Items.SMOKER, Items.TORCH, Items.SOUL_TORCH}) {
                    var stack = new ItemStack(item);
                    require(!stack.isItemBarVisible(), "every unused source hides its bar");
                    var full = text(stack.getTooltip(tooltipContext, client.player, TooltipType.BASIC));
                    require(full.stream().anyMatch(line -> line.contains("Durability: 100%")),
                            "hover explains full durability even while the bar is hidden");
                    stack.set(HeatItems.EXPOSURE, HeatItems.limit(item, ExtremeWinter.CONFIG) / 2);
                    var used = text(stack.getTooltip(tooltipContext, client.player, TooltipType.BASIC));
                    require(stack.isItemBarVisible() && used.stream().anyMatch(line -> line.contains("Durability:"))
                            && used.stream().noneMatch(line -> line.contains("Durability: 100%")),
                            "used item shows its bar and changing remaining duration");
                    if (HeatItems.isTorch(stack)) require(used.stream().anyMatch(line -> line.contains("Ready to place in:")),
                            "cooling torch tooltip reports remaining placement cooldown");
                }
                var lava = text(new ItemStack(Items.LAVA_BUCKET).getTooltip(tooltipContext, client.player, TooltipType.BASIC));
                require(lava.stream().anyMatch(line -> line.contains("Source cooling time:")), "lava bucket explains its world cooling rule");
                var sword = new ItemStack(Items.STONE_SWORD);
                sword.setDamage(80);
                require(sword.isItemBarVisible() && text(sword.getTooltip(tooltipContext, client.player, TooltipType.BASIC))
                        .stream().noneMatch(line -> line.contains("Heat source")), "ordinary items keep their own tooltip and durability");
            });
            game.getServer().runOnServer(server -> {
                var inventory = server.getPlayerManager().getPlayerList().getFirst().getInventory();
                inventory.clear();
                var items = new Item[]{Items.CAMPFIRE, Items.CAMPFIRE, Items.FURNACE, Items.FURNACE,
                        Items.TORCH, Items.TORCH, Items.SOUL_TORCH, Items.SOUL_TORCH, Items.STONE_SWORD};
                for (int slot = 0; slot < items.length; slot++) {
                    var stack = new ItemStack(items[slot]);
                    if (slot < 8 && slot % 2 == 1) stack.set(HeatItems.EXPOSURE, HeatItems.limit(items[slot], ExtremeWinter.CONFIG) / 2);
                    if (slot == 8) stack.setDamage(80);
                    inventory.setStack(slot, stack);
                }
                inventory.markDirty();
            });
            context.waitTicks(2);
            context.setScreen(() -> new InventoryScreen(net.minecraft.client.MinecraftClient.getInstance().player));
            context.getInput().setCursorPos(10, 10);
            context.takeScreenshot("heat-bars-1.2.1-" + profile);
            hover(context, 1);
            context.takeScreenshot("campfire-tooltip-en-1.2.1-" + profile);
            language(context, "zh_cn");
            context.runOnClient(client -> require(text(new ItemStack(Items.CAMPFIRE).getTooltip(
                    Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC)).stream()
                    .anyMatch(line -> line.contains("蹲下空手右键")), "Chinese pickup instructions are translated"));
            hover(context, 1);
            context.takeScreenshot("campfire-tooltip-zh-1.2.1-" + profile);
            hover(context, 3);
            context.takeScreenshot("furnace-tooltip-zh-1.2.1-" + profile);
            game.getServer().runOnServer(server -> {
                var inventory = server.getPlayerManager().getPlayerList().getFirst().getInventory();
                var cold = new ItemStack(Items.TORCH);
                cold.set(HeatItems.EXPOSURE, ExtremeWinter.CONFIG.torchExposureSeconds);
                inventory.setStack(5, cold);
                inventory.markDirty();
            });
            context.waitTicks(2);
            hover(context, 5);
            context.takeScreenshot("torch-tooltip-zh-1.2.1-" + profile);
            context.setScreen(() -> null);
            language(context, "en_us");
        }
        ExtremeWinter.LOGGER.info("TEST used-only bars, heat tooltips, Chinese/English hover and ordinary item preservation PASSED");
    }

    private static List<String> text(List<Text> lines) { return lines.stream().map(Text::getString).toList(); }

    private static void hover(ClientGameTestContext context, int slot) {
        double[] position = context.computeOnClient(client -> {
            var window = client.getWindow();
            double scale = window.getScaleFactor();
            return new double[]{((window.getScaledWidth() - 176) / 2 + 8 + slot * 18 + 8) * scale,
                    ((window.getScaledHeight() - 166) / 2 + 142 + 8) * scale};
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTick();
    }

    private static void language(ClientGameTestContext context, String language) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> {
            client.options.language = language;
            client.getLanguageManager().setLanguage(language);
            return client.reloadResources();
        });
        context.waitFor(client -> reload.isDone(), 400);
        reload.join();
        context.waitFor(client -> client.getOverlay() == null, 400);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
