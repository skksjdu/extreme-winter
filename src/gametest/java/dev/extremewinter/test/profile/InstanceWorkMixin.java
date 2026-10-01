package dev.extremewinter.test.profile;

import dev.extremewinter.test.metrics.WinterWorkProfiler;

import dev.extremewinter.environment.WinterEnvironment;
import dev.extremewinter.environment.WinterWeatherController;
import dev.extremewinter.temperature.HeatWeathering;
import dev.extremewinter.temperature.TemperatureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {WinterEnvironment.class, WinterWeatherController.class, HeatWeathering.class, TemperatureManager.class}, remap = false)
public abstract class InstanceWorkMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void beforeTick(CallbackInfo ci) { WinterWorkProfiler.enter(); }
    @Inject(method = "tick", at = @At("RETURN"))
    private void afterTick(CallbackInfo ci) { WinterWorkProfiler.leave(); }
}
