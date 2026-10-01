package dev.extremewinter.mixin;

import dev.extremewinter.environment.Exposure;
import dev.extremewinter.survival.WinterTasks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class WinterRoofProgressMixin {
    @Unique private static final ThreadLocal<Boolean> winter$exposed = new ThreadLocal<>();
    @Inject(method = "place", at = @At("HEAD"))
    private void winter$before(BlockPlaceContext c, CallbackInfoReturnable<InteractionResult> ci) {
        if (c.getPlayer() instanceof ServerPlayer p) winter$exposed.set(Exposure.outdoors(p.level(), p.blockPosition().above()));
    }
    @Inject(method = "place", at = @At("RETURN"))
    private void winter$after(BlockPlaceContext c, CallbackInfoReturnable<InteractionResult> ci) {
        if (c.getPlayer() instanceof ServerPlayer p) {
            boolean before = Boolean.TRUE.equals(winter$exposed.get()); winter$exposed.remove();
            if (before && ci.getReturnValue().consumesAction() && !Exposure.outdoors(p.level(), p.blockPosition().above())) WinterTasks.award(p, "roof");
        }
    }
}
