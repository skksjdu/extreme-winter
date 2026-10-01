package dev.extremewinter.survival;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Copy the input stack, including every vanilla and third-party component. */
public final class ThermalLiningRecipe extends CustomRecipe {
    public static final ThermalLiningRecipe INSTANCE = new ThermalLiningRecipe();
    public static final RecipeSerializer<ThermalLiningRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE), StreamCodec.<RegistryFriendlyByteBuf, ThermalLiningRecipe>unit(INSTANCE));
    @Override public boolean matches(CraftingInput input, Level level) {
        int armor = 0, lining = 0;
        for (var stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(WinterItems.THERMAL_LINING)) lining++;
            else if (WinterGear.canLine(stack) && !WinterGear.lined(stack)) armor++;
            else return false;
        }
        return armor == 1 && lining == 1;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        if (!matches(input, null)) return ItemStack.EMPTY;
        for (var stack : input.items()) if (WinterGear.canLine(stack)) {
            var result = stack.copyWithCount(1);
            result.set(WinterGear.INSULATION, true);
            return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<ThermalLiningRecipe> getSerializer() { return SERIALIZER; }
}
