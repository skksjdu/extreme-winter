package dev.extremewinter.mixin;

import dev.extremewinter.survival.WinterTasks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class WinterHarvestMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void winter$beginHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        WinterTasks.beginHarvest(player, pos, level.getBlockState(pos));
    }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void winter$finishHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> ci) { WinterTasks.finishHarvest(ci.getReturnValue()); }
}
