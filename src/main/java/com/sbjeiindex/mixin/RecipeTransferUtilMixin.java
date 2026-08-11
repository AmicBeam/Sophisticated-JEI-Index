package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiSlotResolver;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeTransferUtil.class, remap = false)
public class RecipeTransferUtilMixin {
    /**
     * Recent JEI versions validate the raw ids in transfer operations before
     * resolving them to slots. Backpack slots intentionally live outside the
     * menu's slot range, so allow only ids registered in the active resolver.
     */
    @Inject(method = "isValidSlotId", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void sbjeiindex_isValidSlotId(
        AbstractContainerMenu container,
        int slotId,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (JeiSlotResolver.resolve(slotId) != null) {
            cir.setReturnValue(true);
        }
    }
}
