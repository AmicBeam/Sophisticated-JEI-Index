package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.jei.BackpackSnapshotCache;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.jei.transfer.CraftTerminalRecipeTransferHandler", remap = false)
public class CraftTerminalRecipeTransferHandlerMixin {
    @Inject(method = "transferRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_transferRecipe(
        @Coerce Object container,
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

        List<net.neoforged.neoforge.items.IItemHandlerModifiable> handlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }

        List<Slot> inputSources = new ArrayList<>();
        try {
            int craftSlotStartIndex = (int) container.getClass().getField("craftSlotStartIndex").get(container);
            int craftSlotEndIndex = (int) container.getClass().getField("craftSlotEndIndex").get(container);
            for (int i = craftSlotStartIndex; i < craftSlotEndIndex; i++) {
                inputSources.add((Slot) container.getClass().getMethod("getSlot", int.class).invoke(container, i));
            }
        } catch (Exception e) {
            return;
        }

        List<ItemStack> baseInv = player.getInventory().items;
        ArrayList<ItemStack> expanded = new ArrayList<>(baseInv.size() + 64);
        expanded.addAll(baseInv);

        if (!doTransfer) {
            BackpackSnapshotCache.BackpackSnapshot snapshot = BackpackSnapshotCache.getOrCreate((net.minecraft.world.inventory.AbstractContainerMenu) container, handlers);
            expanded.addAll(snapshot.nonEmptyStacks().values());
        } else {
            for (net.neoforged.neoforge.items.IItemHandlerModifiable handler : handlers) {
                int slots = handler.getSlots();
                for (int i = 0; i < slots; i++) {
                    ItemStack s = handler.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        expanded.add(s.copy());
                    }
                }
            }
        }

        Object storage;
        try {
            storage = container.getClass().getField("storage").get(container);
        } catch (Exception e) {
            return;
        }
        Object storageWrapper;
        try {
            storageWrapper = storage.getClass().getMethod("getStorage").invoke(storage);
        } catch (Exception e) {
            return;
        }

        try {
            Class<?> helperClass = Class.forName("com.wintercogs.beyonddimensions.integration.module.jei.transfer.TransferHelper");
            Object error = helperClass.getMethod(
                "transferRecipe",
                List.class,
                Object.class,
                List.class,
                IRecipeSlotsView.class,
                boolean.class,
                boolean.class
            ).invoke(null, inputSources, storageWrapper, expanded, recipeSlots, maxTransfer, doTransfer);
            if (error instanceof IRecipeTransferError e) {
                cir.setReturnValue(e);
            }
        } catch (Exception e) {
            return;
        }
    }
}
