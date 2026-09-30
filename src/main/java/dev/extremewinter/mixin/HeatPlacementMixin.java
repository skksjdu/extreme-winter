package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatItems;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class HeatPlacementMixin {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"), cancellable = true)
    private void winter$waitForTorch(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> result) {
        if (!HeatItems.canPlaceTorch(context.getStack(), context.getPlayer())) result.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "copyComponentsToBlockEntity", at = @At("RETURN"))
    private static void winter$restoreClock(World world, BlockPos pos, ItemStack stack, CallbackInfo info) {
        HeatItems.onPlaced(world, pos, stack);
    }
}
