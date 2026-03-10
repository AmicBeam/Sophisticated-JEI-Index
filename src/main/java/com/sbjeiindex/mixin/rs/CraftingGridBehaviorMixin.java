package com.sbjeiindex.mixin.rs;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackExtraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.grid.CraftingGridBehavior", remap = false)
public class CraftingGridBehaviorMixin {
    @Inject(method = "onRecipeTransfer", at = @At("TAIL"), remap = false)
    private void sbjeiindex_onRecipeTransfer(@Coerce Object grid, Player player, ItemStack[][] recipe, CallbackInfo ci) {
        if (grid == null || player == null || recipe == null) {
            return;
        }

        Object gridType;
        try {
            gridType = grid.getClass().getMethod("getGridType").invoke(grid);
        } catch (Exception e) {
            return;
        }

        if (gridType == null) {
            return;
        }

        String gridTypeName;
        try {
            gridTypeName = (String) gridType.getClass().getMethod("name").invoke(gridType);
        } catch (Exception e) {
            gridTypeName = String.valueOf(gridType);
        }

        if (!"CRAFTING".equals(gridTypeName)) {
            return;
        }

        CraftingContainer matrix;
        try {
            matrix = (CraftingContainer) grid.getClass().getMethod("getCraftingMatrix").invoke(grid);
        } catch (Exception e) {
            return;
        }

        if (matrix == null) {
            return;
        }

        IBackpackWrapper wrapper = BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
        if (wrapper == null) {
            return;
        }

        InventoryHandler handler = wrapper.getInventoryHandler();
        int matrixSlots = Math.min(matrix.getContainerSize(), recipe.length);

        for (int i = 0; i < matrixSlots; i++) {
            if (!matrix.getItem(i).isEmpty()) {
                continue;
            }
            ItemStack[] possibilities = recipe[i];
            if (possibilities == null || possibilities.length == 0) {
                continue;
            }

            ItemStack extracted = BackpackExtraction.extractFirstTemplateMatch(handler, possibilities);
            if (!extracted.isEmpty()) {
                matrix.setItem(i, extracted);
            }
        }
    }
}
