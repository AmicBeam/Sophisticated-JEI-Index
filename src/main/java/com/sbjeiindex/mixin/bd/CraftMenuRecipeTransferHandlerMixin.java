package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.jei.BackpackSnapshotCache;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.jei.transfer.CraftMenuRecipeTransferHandler", remap = false)
public class CraftMenuRecipeTransferHandlerMixin {
    @ModifyArg(
        method = "transferRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/wintercogs/beyonddimensions/integration/module/jei/transfer/TransferHelper;transferRecipe(Ljava/util/List;Ljava/util/List;Ljava/util/List;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
            remap = false
        ),
        index = 2
    )
    private List<ItemStack> sbjeiindex_expandPlayerInvForBackpacks(
        List<ItemStack> original,
        Object container,
        Object recipe,
        Object recipeSlots,
        Player player,
        boolean maxTransfer,
        boolean doTransfer
    ) {
        if (player == null) {
            return original;
        }

        List<IItemHandlerModifiable> handlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return original;
        }

        ArrayList<ItemStack> expanded = new ArrayList<>(original.size() + 64);
        expanded.addAll(original);

        if (!doTransfer && container instanceof AbstractContainerMenu menu) {
            BackpackSnapshotCache.BackpackSnapshot snapshot = BackpackSnapshotCache.getOrCreate(menu, handlers);
            expanded.addAll(snapshot.nonEmptyStacks().values());
            return expanded;
        }

        for (IItemHandlerModifiable handler : handlers) {
            int slots = handler.getSlots();
            for (int i = 0; i < slots; i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    expanded.add(s.copy());
                }
            }
        }

        return expanded;
    }
}
