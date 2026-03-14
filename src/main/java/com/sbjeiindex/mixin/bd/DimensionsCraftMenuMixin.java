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
    @Inject(method = "extractFromInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_extractFromInventory(Inventory inventory, ItemStack template, int amount, CallbackInfoReturnable<Integer> cir) {
        if (amount <= 0 || template == null || template.isEmpty() || inventory == null) {
            cir.setReturnValue(amount);
            return;
        }
        int remaining = amount;
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(inventory.player);
        if (!handlers.isEmpty()) {
            for (InventoryHandler handler : handlers) {
                if (handler == null) {
                    continue;
                }
                int slots = handler.getSlots();
                for (int i = 0; i < slots && remaining > 0; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (ItemStack.isSameItemSameComponents(stack, template)) {
                        int extract = Math.min(remaining, stack.getCount());
                        ItemStack extracted = handler.extractItem(i, extract, false);
                        if (!extracted.isEmpty()) {
                            remaining -= extracted.getCount();
                        }
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                int extract = Math.min(remaining, stack.getCount());
                stack.shrink(extract);
                remaining -= extract;
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        cir.setReturnValue(remaining);
    }
}
