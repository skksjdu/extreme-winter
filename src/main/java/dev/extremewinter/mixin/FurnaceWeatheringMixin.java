package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatWeathering;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent automatic fuel ignition after wind extinguishes a furnace. No rendering hooks. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceWeatheringMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void winter$pauseExposedFurnace(ServerLevel world, BlockPos pos, BlockState state,
                                                  AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (HeatWeathering.furnaceBlocked(world, pos, furnace)) ci.cancel();
    }
}
