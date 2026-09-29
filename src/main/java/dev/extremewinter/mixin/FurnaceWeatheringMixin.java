package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatWeathering;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent automatic fuel ignition after wind extinguishes a furnace. No rendering hooks. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceWeatheringMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void winter$pauseExposedFurnace(ServerWorld world, BlockPos pos, BlockState state,
                                                  AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (HeatWeathering.furnaceBlocked(world, pos, furnace)) ci.cancel();
    }
}
