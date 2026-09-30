package dev.extremewinter.environment;

import dev.extremewinter.ExtremeWinter;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class WinterBlocks {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "snow_drift");
    public static final SnowDriftBlock SNOW_DRIFT = Registry.register(BuiltInRegistries.BLOCK, ID,
            new SnowDriftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.1f).requiresCorrectToolForDrops().sound(SoundType.SNOW).randomTicks().noOcclusion()
                    .dynamicShape().speedFactor(0.85f)
                    .overrideLootTable(Optional.of(ResourceKey.create(Registries.LOOT_TABLE,
                            Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "blocks/snow_drift"))))
                    .setId(ResourceKey.create(Registries.BLOCK, ID))));

    private WinterBlocks() { }
    public static void initialize() {
        Registry.register(BuiltInRegistries.ITEM, ID, new BlockItem(SNOW_DRIFT,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    }
}
