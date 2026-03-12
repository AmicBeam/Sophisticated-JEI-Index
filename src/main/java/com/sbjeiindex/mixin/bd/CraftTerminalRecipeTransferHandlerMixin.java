package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.jei.BackpackSnapshotCache;
import com.sbjeiindex.util.BackpackHelper;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import com.wintercogs.beyonddimensions.integration.module.jei.transfer.TransferHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.jei.transfer.CraftTerminalRecipeTransferHandler", remap = false)
public class CraftTerminalRecipeTransferHandlerMixin {
    @Inject(method = "transferRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_transferRecipe(
        DimensionsCraftMenuTerminal container,
        CraftingRecipe recipe,
        IRecipeSlotsView recipeSlots,
        Player player,
        boolean maxTransfer,
        boolean doTransfer,
        CallbackInfoReturnable<IRecipeTransferError> cir
    ) {
        if (container == null || player == null) {
            return;
        }

        List<IItemHandlerModifiable> handlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }

        List<Slot> inputSources = new ArrayList<>();
        for (int i = container.craftSlotStartIndex; i < container.craftSlotEndIndex; i++) {
            inputSources.add(container.getSlot(i));
        }

        List<ItemStack> baseInv = player.getInventory().items;
        ArrayList<ItemStack> expanded = new ArrayList<>(baseInv.size() + 64);
        expanded.addAll(baseInv);

        if (!doTransfer) {
            BackpackSnapshotCache.BackpackSnapshot snapshot = BackpackSnapshotCache.getOrCreate(container, handlers);
            expanded.addAll(snapshot.nonEmptyStacks().values());
        } else {
            for (IItemHandlerModifiable handler : handlers) {
                int slots = handler.getSlots();
                for (int i = 0; i < slots; i++) {
                    ItemStack s = handler.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        expanded.add(s.copy());
                    }
                }
            }
        }

        IRecipeTransferError error = TransferHelper.transferRecipe(
            inputSources,
            container.storage.getStorage(),
            expanded,
            recipeSlots,
            maxTransfer,
            doTransfer
        );
        cir.setReturnValue(error);
    }
}

