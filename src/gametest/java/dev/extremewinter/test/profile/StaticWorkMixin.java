package dev.extremewinter.test.profile;

import dev.extremewinter.test.metrics.WinterWorkProfiler;

import dev.extremewinter.temperature.WinterWorldState;
import dev.extremewinter.survival.HeatingStoveBlockEntity;
import dev.extremewinter.survival.WinterTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {WinterWorldState.class, HeatingStoveBlockEntity.class, WinterTasks.class}, remap = false)
public abstract class StaticWorkMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private static void beforeTick(CallbackInfo ci) { WinterWorkProfiler.enter(); }
    @Inject(method = "tick", at = @At("RETURN"))
    private static void afterTick(CallbackInfo ci) { WinterWorkProfiler.leave(); }
}
