package dev.extremewinter.survival;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class HeatingContent {
    public static final DataComponentType<Integer> REMAINING = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            WinterGear.id("stove_remaining_ticks"), DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 86400)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final HeatingStoveBlock STOVE = Registry.register(BuiltInRegistries.BLOCK, WinterGear.id("heating_stove"),
            new HeatingStoveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(HeatingStoveBlock.LIT) ? 13 : 0)
                    .setId(ResourceKey.create(Registries.BLOCK, WinterGear.id("heating_stove")))));
    public static final Item STOVE_ITEM = Registry.register(BuiltInRegistries.ITEM, WinterGear.id("heating_stove"),
            new BlockItem(STOVE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WinterGear.id("heating_stove"))).useBlockDescriptionPrefix()));
    public static final BlockEntityType<HeatingStoveBlockEntity> STOVE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            WinterGear.id("heating_stove"), FabricBlockEntityTypeBuilder.create(HeatingStoveBlockEntity::new, STOVE).build());
    public static final MenuType<HeatingStoveMenu> STOVE_MENU = Registry.register(BuiltInRegistries.MENU, WinterGear.id("heating_stove"),
            new MenuType<>(HeatingStoveMenu::new, FeatureFlagSet.of()));
    private HeatingContent() { }
    public static void initialize() {
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(STOVE_ITEM));
    }
}
