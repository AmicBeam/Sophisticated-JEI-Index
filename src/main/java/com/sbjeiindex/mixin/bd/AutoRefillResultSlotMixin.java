package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.widget.slot.AutoRefillResultSlot", remap = false)
public class AutoRefillResultSlotMixin {

    /**
     * 注入到craftSlots.removeItem(slotIndex, itemsToRemove)语句，使其实际从槽位移除物品前，检查一次背包。
     * 此Mixin目的是使得在从维度网络中合成物品时，能连续从背包中消耗物品
     */
    @Redirect(
            method = "onTake",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
            ),
            remap = false
    )
    private ItemStack sbjeiindex_extractFromBackpacksBeforeRemoving(
            CraftingContainer craftSlots,
            int slotIndex,
            int amount,
            Player player,
            ItemStack craftedStack
    ) {
        int remaining = amount;
        if (remaining > 0 && player != null) {
            ItemStack template = craftSlots.getItem(slotIndex);
            if (!template.isEmpty()) {
                remaining = sb_jei_index$extractFromBackpacks(player, template, remaining);
            }
        }

        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        return craftSlots.removeItem(slotIndex, remaining);
    }

    @Unique
    private static int sb_jei_index$extractFromBackpacks(Player player, ItemStack template, int amount) {
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return amount;
        }

        int remaining = amount;
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
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
        return remaining;
    }
}
