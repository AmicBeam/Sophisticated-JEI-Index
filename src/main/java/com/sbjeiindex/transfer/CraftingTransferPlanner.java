package com.sbjeiindex.transfer;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CraftingTransferPlanner {
    private CraftingTransferPlanner() {}

    public record Plan(List<ItemStack> templates, int sets) {}

    @Nullable
    public static Plan plan(List<List<ItemStack>> candidatesBySlot, List<ItemStack> stacks, boolean maxTransfer) {
        List<ItemStack> types = mergeTypes(stacks);
        int high = maxTransfer ? 64 : 1;
        Plan best = null;
        int low = 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            List<ItemStack> selected = select(candidatesBySlot, types, middle);
            if (selected != null) {
                best = new Plan(selected, middle);
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return best;
    }

    @Nullable
    private static List<ItemStack> select(List<List<ItemStack>> candidatesBySlot, List<ItemStack> types, int sets) {
        List<Cell> cells = new ArrayList<>();
        for (int slot = 0; slot < Math.min(9, candidatesBySlot.size()); slot++) {
            List<ItemStack> displayed = candidatesBySlot.get(slot);
            if (displayed.isEmpty()) {
                continue;
            }
            List<Integer> candidates = new ArrayList<>();
            for (int type = 0; type < types.size(); type++) {
                ItemStack available = types.get(type);
                if (displayed.stream().anyMatch(candidate -> !candidate.isEmpty() && candidate.is(available.getItem()))
                    && available.getCount() >= sets) {
                    candidates.add(type);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            cells.add(new Cell(slot, candidates));
        }
        cells.sort(Comparator.comparingInt(cell -> cell.candidates().size()));
        int[] used = new int[types.size()];
        int[] selected = new int[9];
        java.util.Arrays.fill(selected, -1);
        if (!select(cells, 0, types, used, selected, sets)) {
            return null;
        }
        List<ItemStack> templates = new ArrayList<>(9);
        for (int type : selected) {
            templates.add(type < 0 ? ItemStack.EMPTY : types.get(type).copyWithCount(1));
        }
        return List.copyOf(templates);
    }

    private static boolean select(List<Cell> cells, int index, List<ItemStack> types,
                                  int[] used, int[] selected, int sets) {
        if (index == cells.size()) {
            return true;
        }
        Cell cell = cells.get(index);
        for (int type : cell.candidates()) {
            if (used[type] + sets > types.get(type).getCount()) {
                continue;
            }
            used[type] += sets;
            selected[cell.slot()] = type;
            if (select(cells, index + 1, types, used, selected, sets)) {
                return true;
            }
            selected[cell.slot()] = -1;
            used[type] -= sets;
        }
        return false;
    }

    private static List<ItemStack> mergeTypes(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack existing = result.stream()
                .filter(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))
                .findFirst().orElse(null);
            if (existing == null) {
                result.add(stack.copy());
            } else {
                existing.grow(stack.getCount());
            }
        }
        return result;
    }

    private record Cell(int slot, List<Integer> candidates) {}
}
