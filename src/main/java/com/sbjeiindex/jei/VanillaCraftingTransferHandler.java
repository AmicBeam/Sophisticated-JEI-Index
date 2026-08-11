package com.sbjeiindex.jei;

import com.sbjeiindex.network.VanillaRecipeTransferPayload;
import com.sbjeiindex.transfer.CraftingTransferPlanner;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VanillaCraftingTransferHandler implements IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> vanillaDelegate;

    public VanillaCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        var info = helper.createBasicRecipeTransferInfo(
            CraftingMenu.class, MenuType.CRAFTING, RecipeTypes.CRAFTING, 1, 9, 10, 36
        );
        this.vanillaDelegate = helper.createUnregisteredRecipeTransferHandler(info);
    }

    @Override
    public Class<? extends CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(CraftingMenu menu, RecipeHolder<CraftingRecipe> recipe,
                                                IRecipeSlotsView recipeSlots, Player player,
                                                boolean maxTransfer, boolean doTransfer) {
        var backpacks = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (backpacks.isEmpty()) {
            return vanillaDelegate.transferRecipe(menu, recipe, recipeSlots, player, maxTransfer, doTransfer);
        }
        if (!helper.recipeTransferHasServerSupport()) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.no.server"));
        }

        List<ItemStack> available = new ArrayList<>();
        for (int slot = 1; slot <= 45; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        backpacks.forEach(handler -> {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    available.add(stack.copy());
                }
            }
        });

        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(mezz.jei.api.recipe.RecipeIngredientRole.INPUT);
        List<List<ItemStack>> candidates = inputSlots.stream()
            .map(slot -> slot.getItemStacks().toList())
            .toList();
        CraftingTransferPlanner.Plan plan = CraftingTransferPlanner.plan(candidates, available, maxTransfer);
        if (plan == null) {
            List<IRecipeSlotView> missing = inputSlots.stream()
                .filter(slot -> !slot.isEmpty())
                .filter(slot -> available.stream().noneMatch(stack -> slot.getItemStacks().anyMatch(candidate -> sameItem(candidate, stack))))
                .toList();
            if (missing.isEmpty()) {
                missing = inputSlots.stream().limit(1).toList();
            }
            if (!missing.isEmpty()) {
                return helper.createUserErrorForMissingSlots(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missing
                );
            }
            return helper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.missing"));
        }

        if (doTransfer) {
            PacketDistributor.sendToServer(new VanillaRecipeTransferPayload(
                menu.containerId, recipe.id(), plan.templates(), maxTransfer
            ));
        }
        return null;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && first.is(second.getItem());
    }
}
