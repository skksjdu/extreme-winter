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

    private StarterSupplies() { }
    public static void initialize() { }

    public static void onJoin(ServerPlayer player) {
        if (player.getAttachedOrCreate(RECEIVED)) return;
        player.setAttached(RECEIVED, true);
        if (player.getAttachedOrCreate(LEGACY_ARRIVED)
                || player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) return;
        var campfire = new ItemStack(Items.CAMPFIRE);
        if (!player.getInventory().add(campfire)) player.drop(campfire, false);
        player.getInventory().setChanged();
    }
}
