package dev.extremewinter.survival;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Per-player, per-save receipt; no terrain generation, spawn changes or teleportation. */
public final class StarterSupplies {
    private static final AttachmentType<Boolean> RECEIVED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "starter_heat_received"),
            builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    // Read old receipts so upgrading an existing save never adds a second starter kit.
    private static final AttachmentType<Boolean> LEGACY_ARRIVED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "shelter_arrival"),
            builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());

    private static final AttachmentType<Boolean> GUIDED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "beginner_guidance_received"),
            b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    private StarterSupplies() { }
    private static final AttachmentType<Boolean> MANUAL_RECEIVED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "survival_manual_received"),
            b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());
    public static void initialize() { }

    public static void onJoin(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(WinterItems.HOT_WATER_BOTTLE)) {
                stack.set(WinterGear.CHARGE, 0);
                stack.remove(WinterGear.LAST_CHARGE);
            }
        }
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getAttachedOrCreate(GUIDED)) {
            player.setAttached(GUIDED, true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.extreme_winter.beginner"));
        }
        if (!player.getAttachedOrCreate(RECEIVED)) {
            player.setAttached(RECEIVED, true);
            if (!player.getAttachedOrCreate(LEGACY_ARRIVED)
                    && player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) == 0) {
                for (var stack : java.util.List.of(new ItemStack(Items.CAMPFIRE), new ItemStack(Items.TORCH, 16),
                        new ItemStack(Items.STONE_SHOVEL), new ItemStack(Items.BAKED_POTATO, 4))) {
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                }
            }
        }
        // Append after the old kit so the established hotbar order remains stable.
        if (!player.getAttachedOrCreate(MANUAL_RECEIVED)) {
            player.setAttached(MANUAL_RECEIVED, true);
            var manual = new ItemStack(WinterItems.SURVIVAL_MANUAL);
            if (!player.getInventory().add(manual)) player.drop(manual, false);
        }
        player.getInventory().setChanged();
    }
}
