package dev.extremewinter.mixin;

import dev.extremewinter.survival.WinterTasks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweetBerryBushBlock.class)
public abstract class BerryHarvestMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void winter$begin(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof ServerPlayer p) WinterTasks.beginHarvest(p, pos, state);
    }
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void winter$finish(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof ServerPlayer) WinterTasks.finishHarvest(ci.getReturnValue().consumesAction());
    }
}
