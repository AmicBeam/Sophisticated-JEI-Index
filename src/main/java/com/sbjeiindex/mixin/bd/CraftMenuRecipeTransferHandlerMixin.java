package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

import java.util.ArrayList;
import java.util.List;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.jei.transfer.CraftMenuRecipeTransferHandler", remap = false)
public class CraftMenuRecipeTransferHandlerMixin {
    @ModifyArg(
        method = "transferRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/wintercogs/beyonddimensions/integration/module/jei/transfer/TransferHelper;transferRecipe(Ljava/util/List;Ljava/util/List;Ljava/util/List;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;"
        ),
        index = 2,
        remap = false
    )
    private List<ItemStack> sbjeiindex_extendPlayerInv(
        List<ItemStack> playerInv,
        @Coerce Object container,
        @Coerce Object recipe,
        IRecipeSlotsView recipeSlots,
        Player player,
        boolean maxTransfer,
        boolean doTransfer
    ) {
        List<ItemStack> combined = new ArrayList<>(playerInv);
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return combined;
        }
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    combined.add(stack);
                }
            }
        }
        return combined;
    }
}
