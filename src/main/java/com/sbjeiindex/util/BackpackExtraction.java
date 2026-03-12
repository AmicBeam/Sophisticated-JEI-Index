package com.sbjeiindex.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BackpackExtraction {
    private BackpackExtraction() {
    }

    public static List<int[]> createReservedCounts(List<? extends InventoryHandler> handlers) {
        List<int[]> reserved = new ArrayList<>(handlers.size());
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            reserved.add(new int[handler.getSlots()]);
        }
        return reserved;
    }

    public static ItemStack extractIngredient(List<? extends InventoryHandler> handlers, Ingredient ingredient) {
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            ItemStack extracted = extractIngredient(handler, ingredient);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extractFirstTemplateMatch(List<? extends InventoryHandler> handlers, ItemStack[] templates) {
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            ItemStack extracted = extractFirstTemplateMatch(handler, templates);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean reserveIngredient(List<? extends InventoryHandler> handlers, List<int[]> reservedCounts, Ingredient ingredient) {
        int n = Math.min(handlers.size(), reservedCounts.size());
        for (int i = 0; i < n; i++) {
            InventoryHandler handler = handlers.get(i);
            int[] reserved = reservedCounts.get(i);
            if (handler == null || reserved == null) {
                continue;
            }
            if (reserveIngredient(handler, reserved, ingredient)) {
                return true;
            }
        }
        return false;
    }

    public static boolean reserveFirstTemplateMatch(List<? extends InventoryHandler> handlers, List<int[]> reservedCounts, ItemStack[] templates) {
        int n = Math.min(handlers.size(), reservedCounts.size());
        for (int i = 0; i < n; i++) {
            InventoryHandler handler = handlers.get(i);
            int[] reserved = reservedCounts.get(i);
            if (handler == null || reserved == null) {
                continue;
            }
            if (reserveFirstTemplateMatch(handler, reserved, templates)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAnyTemplateMatch(List<? extends InventoryHandler> handlers, ItemStack[] templates) {
        for (InventoryHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            if (containsAnyTemplateMatch(handler, templates)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAnyTemplateMatch(InventoryHandler handler, ItemStack[] templates) {
        if (handler == null) {
            return false;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
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

    public static boolean reserveIngredient(InventoryHandler handler, int[] reservedCounts, Ingredient ingredient) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            int available = stack.getCount() - reservedCounts[i];
            if (available > 0 && ingredient.test(stack)) {
                reservedCounts[i]++;
                return true;
            }
        }
        return false;
    }

    public static boolean reserveFirstTemplateMatch(InventoryHandler handler, int[] reservedCounts, ItemStack[] templates) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            int available = stack.getCount() - reservedCounts[i];
            if (available <= 0) {
                continue;
            }
            for (ItemStack template : templates) {
                if (template != null && !template.isEmpty() && ItemStack.isSameItemSameTags(stack, template)) {
                    reservedCounts[i]++;
                    return true;
                }
            }
        }
        return false;
    }
}
