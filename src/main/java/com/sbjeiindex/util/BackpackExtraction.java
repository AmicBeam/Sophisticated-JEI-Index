package com.sbjeiindex.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.function.Predicate;

public class BackpackExtraction {
    private BackpackExtraction() {
    }

    public static ItemStack extractFirstMatching(InventoryHandler handler, Predicate<ItemStack> matcher) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && matcher.test(stack)) {
                ItemStack extracted = handler.extractItem(i, 1, false);
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extractIngredient(InventoryHandler handler, Ingredient ingredient) {
        return extractFirstMatching(handler, ingredient::test);
    }

    public static ItemStack extractFirstTemplateMatch(InventoryHandler handler, ItemStack[] templates) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (ItemStack template : templates) {
                if (template != null && !template.isEmpty() && ItemStack.isSameItemSameTags(stack, template)) {
                    ItemStack extracted = handler.extractItem(i, 1, false);
                    if (!extracted.isEmpty()) {
                        return extracted;
                    }
                    break;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}

