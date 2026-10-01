package dev.extremewinter.mixin;

import dev.extremewinter.survival.WinterGear;
import dev.extremewinter.survival.WinterTasks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class WinterCraftingProgressMixin {
    @Inject(method = "onTake", at = @At("RETURN"))
    private void winter$crafted(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer p && WinterGear.lined(stack)) WinterTasks.award(p, "lining");
    }
}
