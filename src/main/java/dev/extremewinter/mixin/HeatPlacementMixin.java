package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class HeatPlacementMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void winter$waitForTorch(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> result) {
        if (!HeatItems.canPlaceTorch(context.getItemInHand(), context.getPlayer())) result.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "updateBlockEntityComponents", at = @At("RETURN"))
    private static void winter$restoreClock(Level world, BlockPos pos, ItemStack stack, CallbackInfo info) {
        HeatItems.onPlaced(world, pos, stack);
    }
}
