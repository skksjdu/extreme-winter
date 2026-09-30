package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the normal inventory/hotbar durability renderer, without replacing GUI rendering. */
@Mixin(Item.class)
public abstract class HeatItemBarMixin {
    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void winter$visible(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(HeatItems.elapsed(stack) > 0);
    }
    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void winter$step(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(HeatItems.barStep(stack));
    }
    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void winter$color(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        if (HeatItems.supported(stack)) result.setReturnValue(HeatItems.barColor(stack));
    }
}
