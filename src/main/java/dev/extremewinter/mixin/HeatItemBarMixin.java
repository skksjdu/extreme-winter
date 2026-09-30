package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the normal inventory/hotbar durability renderer, without replacing GUI rendering. */
@Mixin(Item.class)
public abstract class HeatItemBarMixin {
    @Inject(method = "isItemBarVisible", at = @At("HEAD"), cancellable = true)
    private void winter$visible(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(true);
    }
    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void winter$step(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(HeatItems.barStep(stack));
    }
    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void winter$color(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(HeatItems.barColor(stack));
    }
}
