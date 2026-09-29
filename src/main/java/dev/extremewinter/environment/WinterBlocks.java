package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import java.util.Optional;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class WinterBlocks {
    private static final Identifier ID = Identifier.of(ExtremeWinter.ID, "snow_drift");
    public static final SnowDriftBlock SNOW_DRIFT = Registry.register(Registries.BLOCK, ID,
            new SnowDriftBlock(AbstractBlock.Settings.create().mapColor(MapColor.WHITE)
                    .strength(0.1f).requiresTool().sounds(BlockSoundGroup.SNOW).ticksRandomly().nonOpaque()
                    .lootTable(Optional.of(RegistryKey.of(RegistryKeys.LOOT_TABLE,
                            Identifier.of(ExtremeWinter.ID, "blocks/snow_drift"))))
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, ID))));

    private WinterBlocks() { }
    public static void initialize() {
        Registry.register(Registries.ITEM, ID, new BlockItem(SNOW_DRIFT,
                new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, ID)).useBlockPrefixedTranslationKey()));
    }
}
