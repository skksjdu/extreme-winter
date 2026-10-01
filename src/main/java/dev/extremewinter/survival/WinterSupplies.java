package dev.extremewinter.survival;

import java.util.Set;
import dev.extremewinter.ExtremeWinter;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/** Append a single optional pool to a finite list; never replace existing pools or filled chests. */
public final class WinterSupplies {
    public static final Set<String> TABLES = Set.of("chests/village/village_plains_house", "chests/village/village_snowy_house",
            "chests/village/village_taiga_house", "chests/village/village_savanna_house", "chests/village/village_desert_house",
            "chests/abandoned_mineshaft", "chests/shipwreck_supply", "chests/shipwreck_treasure", "chests/shipwreck_map");
    private WinterSupplies() { }
    public static boolean eligible(Identifier id, LootTableSource source) {
        return ExtremeWinter.CONFIG.structureSupplies && source == LootTableSource.VANILLA
                && id.getNamespace().equals("minecraft") && TABLES.contains(id.getPath());
    }
    public static LootPool.Builder pool() {
        // Coal and charcoal split the fuel weight: 5 + 5 versus lining 6, bottle 2, stew 2.
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(1f / 3))
                .add(LootItem.lootTableItem(Items.COAL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                .add(LootItem.lootTableItem(Items.CHARCOAL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                .add(LootItem.lootTableItem(WinterItems.THERMAL_LINING).setWeight(6))
                .add(LootItem.lootTableItem(WinterItems.HOT_WATER_BOTTLE).setWeight(2))
                .add(LootItem.lootTableItem(WinterItems.WARMING_STEW).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))));
    }
    public static void initialize() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> { if (eligible(key.identifier(), source)) table.withPool(pool()); });
    }
}
