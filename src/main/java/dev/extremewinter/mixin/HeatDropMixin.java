package dev.extremewinter.mixin;

import java.util.List;
import dev.extremewinter.temperature.HeatItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class HeatDropMixin {
    @Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void winter$preserveClock(BlockState state, ServerWorld world, BlockPos pos,
            BlockEntity entity, Entity breaker, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> result) {
        if (entity != null) for (var stack : result.getReturnValue()) HeatItems.copyToDrop(entity, stack);
    }
}
