package com.sbjeiindex.emi;

import com.sbjeiindex.network.VanillaRecipeTransferPayload;
import com.sbjeiindex.transfer.CraftingTransferPlanner;
import com.sbjeiindex.util.BackpackHelper;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VanillaCraftingEmiHandler implements EmiRecipeHandler<CraftingMenu> {
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<CraftingMenu> screen) {
        List<EmiStack> stacks = collectAvailable(screen.getMenu()).stream().map(EmiStack::of).toList();
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        var player = Minecraft.getInstance().player;
        return player != null
            && !BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player).isEmpty()
            && recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING
            && recipe.supportsRecipeTree()
            && recipe.getId() != null;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
        CraftingTransferPlanner.Plan plan = createPlan(recipe, context.getScreenHandler(), context.getAmount() > 1);
        return plan != null && (context.getAmount() == Integer.MAX_VALUE || plan.sets() >= Math.max(1, context.getAmount()));
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
        CraftingTransferPlanner.Plan plan = createPlan(recipe, context.getScreenHandler(), context.getAmount() > 1);
        ResourceLocation recipeId = recipe.getId();
        if (plan == null || recipeId == null) {
            return false;
        }
        int action = switch (context.getDestination()) {
            case CURSOR -> 1;
            case INVENTORY -> 2;
            default -> 0;
        };
        com.sbjeiindex.network.ModPayloads.CHANNEL.sendToServer(new VanillaRecipeTransferPayload(
            context.getScreenHandler().containerId, recipeId, plan.templates(), context.getAmount() > 1,
            context.getAmount() == Integer.MAX_VALUE ? 0 : Math.max(1, context.getAmount()), action
        ));
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }

    private static CraftingTransferPlanner.Plan createPlan(EmiRecipe recipe, CraftingMenu menu, boolean maxTransfer) {
        List<List<ItemStack>> candidates = new ArrayList<>(9);
        List<EmiIngredient> inputs = recipe.getInputs();
        for (int slot = 0; slot < 9; slot++) {
            if (slot >= inputs.size() || inputs.get(slot).isEmpty()) {
                candidates.add(List.of());
            } else {
                candidates.add(inputs.get(slot).getEmiStacks().stream()
                    .map(EmiStack::getItemStack)
                    .filter(stack -> !stack.isEmpty())
                    .toList());
            }
        }
        return CraftingTransferPlanner.plan(candidates, collectAvailable(menu), maxTransfer);
    }

    private static List<ItemStack> collectAvailable(CraftingMenu menu) {
        List<ItemStack> available = new ArrayList<>();
        for (int slot = 1; slot <= 45; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        var player = Minecraft.getInstance().player;
        if (player != null) {
            BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player).forEach(handler -> {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        available.add(stack.copy());
                    }
                }
            });
        }
        return available;
    }
}
