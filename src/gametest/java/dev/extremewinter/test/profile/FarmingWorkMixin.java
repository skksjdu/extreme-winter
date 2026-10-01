package dev.extremewinter.test.profile;

import dev.extremewinter.test.metrics.WinterWorkProfiler;

import dev.extremewinter.survival.WinterFarming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WinterFarming.class, remap = false)
public abstract class FarmingWorkMixin {
    @Inject(method = "allowGrowth", at = @At("HEAD"))
    private static void beforeGrowth(CallbackInfoReturnable<Boolean> ci) { WinterWorkProfiler.enter(); }
    @Inject(method = "allowGrowth", at = @At("RETURN"))
    private static void afterGrowth(CallbackInfoReturnable<Boolean> ci) { WinterWorkProfiler.leave(); }
}
