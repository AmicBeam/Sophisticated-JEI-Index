package com.sbjeiindex.mixin.ts;

import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(targets = "com.tom.storagemod.jei.CraftingTerminalTransferHandler", remap = false)
public class CraftingTerminalTransferHandlerMixin {
    @Shadow
    private IRecipeTransferHandlerHelper helper;

    private static final IRecipeTransferError INTERNAL_ERROR = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.INTERNAL;
        }
    };

    @Inject(method = "transferRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_transferRecipe(@Coerce Object container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer, CallbackInfoReturnable<IRecipeTransferError> cir) {
        if (container == null || recipe == null || recipeSlots == null || player == null) {
            return;
        }

        Object storedItemsObj;
        try {
            storedItemsObj = container.getClass().getMethod("getStoredItems").invoke(container);
        } catch (Exception e) {
            cir.setReturnValue(INTERNAL_ERROR);
            return;
        }

        Set<?> stored = new HashSet<>();
        if (storedItemsObj instanceof Collection<?> c) {
            stored = new HashSet<>(c);
        }

        IBackpackWrapper backpackWrapper = BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
        InventoryHandler backpack = backpackWrapper == null ? null : backpackWrapper.getInventoryHandler();

        List<IRecipeSlotView> missing = new ArrayList<>();
        for (IRecipeSlotView view : recipeSlots.getSlotViews()) {
            if (view.getRole() != RecipeIngredientRole.INPUT && view.getRole() != RecipeIngredientRole.CATALYST) {
                continue;
            }

            ItemStack[] templates = view.getIngredients(VanillaTypes.ITEM_STACK).toArray(ItemStack[]::new);
            if (templates.length == 0) {
                continue;
            }

            boolean found = hasInPlayerInventory(player, templates)
                || hasInTerminalStorage(stored, templates)
                || hasInBackpack(backpack, templates);

            if (!found) {
                missing.add(view);
            }
        }

        if (doTransfer) {
            try {
                var recipeId = recipe.getId();
                if (recipeId != null && !player.level().getRecipeManager().byKey(recipeId).isEmpty()) {
                    CompoundTag compound = new CompoundTag();
                    compound.putString("fill", recipeId.toString());
                    container.getClass().getMethod("sendMessage", CompoundTag.class).invoke(container, compound);
                }
            } catch (Exception e) {
            }
        }

        if (!missing.isEmpty()) {
            IRecipeTransferError parent = helper.createUserErrorForMissingSlots(Component.translatable("tooltip.toms_storage.items_missing"), missing);
            cir.setReturnValue(new CosmeticError(parent));
        } else {
            cir.setReturnValue(null);
        }
    }

    private static boolean hasInPlayerInventory(Player player, ItemStack[] templates) {
        for (ItemStack template : templates) {
            if (template != null && !template.isEmpty() && player.getInventory().findSlotMatchingItem(template) != -1) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInTerminalStorage(Set<?> stored, ItemStack[] templates) {
        if (stored.isEmpty()) {
            return false;
        }

        Constructor<?> ctor;
        try {
            Class<?> cls = Class.forName("com.tom.storagemod.util.StoredItemStack");
            ctor = cls.getConstructor(ItemStack.class);
        } catch (Exception e) {
            return false;
        }

        for (ItemStack template : templates) {
            if (template == null || template.isEmpty()) {
                continue;
            }
            try {
                Object key = ctor.newInstance(template);
                if (stored.contains(key)) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    private static boolean hasInBackpack(InventoryHandler backpack, ItemStack[] templates) {
        if (backpack == null) {
            return false;
        }

        int slots = backpack.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = backpack.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (ItemStack template : templates) {
                if (template != null && !template.isEmpty() && ItemStack.isSameItemSameTags(stack, template)) {
                    return true;
                }
            }
        }
        return false;
    }

    private record CosmeticError(IRecipeTransferError parent) implements IRecipeTransferError {
        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public void showError(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
            parent.showError(guiGraphics, mouseX, mouseY, recipeSlotsView, recipeX, recipeY);
        }
    }
}

