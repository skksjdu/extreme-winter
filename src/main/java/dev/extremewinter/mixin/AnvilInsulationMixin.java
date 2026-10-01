package dev.extremewinter.mixin;

import dev.extremewinter.survival.WinterGear;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilInsulationMixin {
    @Inject(method = "createResult", at = @At("RETURN"))
    private void winter$keepLining(CallbackInfo ci) {
        var menu = (AnvilMenu) (Object) this;
        var result = menu.getSlot(2).getItem();
        if (!result.isEmpty() && WinterGear.canLine(result)
                && (WinterGear.lined(menu.getSlot(0).getItem()) || WinterGear.lined(menu.getSlot(1).getItem())))
            result.set(WinterGear.INSULATION, true);
    }
}
