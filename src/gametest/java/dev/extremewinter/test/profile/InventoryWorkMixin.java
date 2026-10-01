package dev.extremewinter.test.profile;

import dev.extremewinter.test.metrics.WinterWorkProfiler;

import dev.extremewinter.temperature.HeatItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeatItems.class, remap = false)
public abstract class InventoryWorkMixin {
    @Inject(method = "tickInventories", at = @At("HEAD"))
    private static void beforeTick(CallbackInfo ci) { WinterWorkProfiler.enter(); }
    @Inject(method = "tickInventories", at = @At("RETURN"))
    private static void afterTick(CallbackInfo ci) { WinterWorkProfiler.leave(); }
}
