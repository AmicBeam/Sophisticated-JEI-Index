package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu", remap = false)
public class DimensionsCraftMenuMixin {

    @Inject(method = "extractFromInventory", at = @At("RETURN"), cancellable = true, remap = false)
    private void sbjeiindex_extractFromBackpacks(Inventory inventory, ItemStack template, int amount, CallbackInfoReturnable<Integer> cir) {
        int remaining = cir.getReturnValueI();
        if (remaining <= 0 || template == null || template.isEmpty() || inventory == null || inventory.player == null) {
            return;
        }

        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(inventory.player);
        if (handlers.isEmpty()) {
            return;
        }

        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            int slots = handler.getSlots();
            for (int i = 0; i < slots && remaining > 0; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) {
                    continue;
                }
                ItemStack extracted = handler.extractItem(i, remaining, false);
                if (!extracted.isEmpty()) {
                    remaining -= extracted.getCount();
                }
            }
            if (remaining <= 0) {
                break;
            }
        }

        cir.setReturnValue(remaining);
    }
}
