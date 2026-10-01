package dev.extremewinter.survival;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class WinterItems {
    public static final Item THERMAL_LINING = item("thermal_lining", Item::new, new Item.Properties());
    public static final Item HOT_WATER_BOTTLE = item("hot_water_bottle", HotWaterBottleItem::new, new Item.Properties().stacksTo(1));
    public static final Item WARMING_STEW = item("warming_stew", WarmingStewItem::new,
            new Item.Properties().stacksTo(16).food(new FoodProperties(6, 4.8f, false)).usingConvertsTo(Items.BOWL));
    public static final Item SURVIVAL_MANUAL = item("survival_manual", SurvivalManualItem::new, new Item.Properties().stacksTo(1));
    public static final Item WARMTH_METER = item("warmth_meter", WarmthMeterItem::new, new Item.Properties().stacksTo(1));
    public static final WeatherInstrumentBlock WEATHER_INSTRUMENT = Registry.register(BuiltInRegistries.BLOCK, WinterGear.id("weather_instrument"),
            new WeatherInstrumentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2).noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK, WinterGear.id("weather_instrument")))));
    public static final Item WEATHER_INSTRUMENT_ITEM = item("weather_instrument", p -> new BlockItem(WEATHER_INSTRUMENT, p.useBlockDescriptionPrefix()), new Item.Properties());
    private WinterItems() { }
    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        var id = WinterGear.id(name);
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(properties.setId(ResourceKey.create(Registries.ITEM, id))));
    }
    public static void initialize() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, WinterGear.id("thermal_lining_upgrade"), ThermalLiningRecipe.SERIALIZER);
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> { for (var item : new Item[]{THERMAL_LINING, HOT_WATER_BOTTLE, WARMING_STEW, SURVIVAL_MANUAL, WARMTH_METER, WEATHER_INSTRUMENT_ITEM}) entries.accept(item); });
    }
}
