package dev.extremewinter.survival;

import java.util.stream.IntStream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public final class SurvivalManualItem extends WrittenBookItem {
    public SurvivalManualItem(Properties properties) { super(properties.component(DataComponents.WRITTEN_BOOK_CONTENT, content())); }
    public static WrittenBookContent content() {
        return new WrittenBookContent(Filterable.passThrough("Winter Survival"), "Extreme Winter", 0,
                IntStream.rangeClosed(1, 6).mapToObj(i -> Filterable.<Component>passThrough(
                        Component.translatable("manual.extreme_winter.page" + i))).toList(), true);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Refresh serialized old pages when a later release unlocks a new mechanic.
        player.getItemInHand(hand).set(DataComponents.WRITTEN_BOOK_CONTENT, content());
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) serverPlayer.containerMenu.broadcastChanges();
        return super.use(level, player, hand);
    }
}
