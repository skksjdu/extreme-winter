package dev.extremewinter.mixin;

import dev.extremewinter.temperature.HeatSourceIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class HeatSourceIndexMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void winter$sourceChanged(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> result) {
        var old = result.getReturnValue();
        if (old != null && (HeatSourceIndex.indexed(old) || HeatSourceIndex.indexed(state))
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel world) {
            HeatSourceIndex.changed(world, pos, state);
        }
    }
}
