package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiSlotResolver;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransferOperation.class, remap = false)
public abstract class TransferOperationMixin {
    @Shadow(remap = false)
    public abstract int inventorySlotId();

    @Shadow(remap = false)
    public abstract int craftingSlotId();

    @Inject(method = "inventorySlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_inventorySlot(AbstractContainerMenu container, CallbackInfoReturnable<Slot> cir) {
        Slot resolved = JeiSlotResolver.resolve(inventorySlotId());
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }

    @Inject(method = "craftingSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_craftingSlot(AbstractContainerMenu container, CallbackInfoReturnable<Slot> cir) {
        Slot resolved = JeiSlotResolver.resolve(craftingSlotId());
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }
}
