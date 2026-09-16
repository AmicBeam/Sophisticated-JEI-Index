package com.sbjeiindex.mixin.ts;

import com.sbjeiindex.util.BackpackExtraction;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(targets = "com.tom.storagemod.jei.CraftingTerminalTransferHandler", remap = false)
public class CraftingTerminalTransferHandlerMixin {
    @Redirect(
        method = "transferRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
        ),
        remap = false
    )
    private int sbjeiindex_findSlotMatchingItem(Inventory inventory, ItemStack stack) {
        int id = inventory.findSlotMatchingItem(stack);
        if (id != -1) {
            return id;
        }
        if (inventory == null || inventory.player == null || stack == null || stack.isEmpty()) {
            return -1;
        }
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(inventory.player);
        if (handlers.isEmpty()) {
            return -1;
        }
        ItemStack[] templates = new ItemStack[]{stack};
        return BackpackExtraction.containsAnyTemplateMatch(handlers, templates) ? 0 : -1;
    }
}
