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
     * JEI 15.49 validates raw transfer-operation ids before resolving them to
     * slots. Virtual backpack ids are outside the open menu's slot range, so
     * accept only ids that are present in the active backpack slot resolver.
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
