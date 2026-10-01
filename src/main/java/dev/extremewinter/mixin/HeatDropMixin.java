package dev.extremewinter.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.extremewinter.temperature.HeatItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class HeatDropMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void winter$preserveClock(BlockState state, ServerLevel world, BlockPos pos,
            BlockEntity entity, Entity breaker, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> result) {
        for (var stack : result.getReturnValue()) {
            HeatItems.copyToDrop(world, pos, entity, stack);
            if (entity instanceof dev.extremewinter.survival.HeatingStoveBlockEntity stove) stove.copyToDrop(stack);
        }
    }
}
