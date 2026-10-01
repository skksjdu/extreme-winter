package dev.extremewinter.mixin;

import dev.extremewinter.survival.WinterGear;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class RepairInsulationMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void winter$keepLining(CraftingInput input, CallbackInfoReturnable<ItemStack> result) {
        if (!result.getReturnValue().isEmpty() && input.items().stream().anyMatch(WinterGear::lined))
            result.getReturnValue().set(WinterGear.INSULATION, true);
    }
}
