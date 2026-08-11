package com.sbjeiindex.transfer;

import com.sbjeiindex.network.VanillaRecipeTransferPayload;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VanillaCraftingTransferService {
    private VanillaCraftingTransferService() {}

    public static void transfer(ServerPlayer player, VanillaRecipeTransferPayload payload) {
        if (!(player.containerMenu instanceof CraftingMenu menu)
            || menu.containerId != payload.containerId()
            || payload.templates().size() != 9) {
            return;
        }

        Optional<RecipeHolder<?>> recipe = player.serverLevel().getRecipeManager().byKey(payload.recipeId());
        if (recipe.isEmpty() || !(recipe.get().value() instanceof CraftingRecipe craftingRecipe)) {
            return;
        }
        List<ItemStack> templates = payload.templates().stream()
            .map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1))
            .toList();
        if (!craftingRecipe.matches(CraftingInput.of(3, 3, templates), player.serverLevel())) {
            return;
        }

        List<InventoryHandler> backpacks = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (backpacks.isEmpty()) {
            return;
        }
        Transaction transaction = new Transaction(menu, backpacks);
        int sets = payload.maxTransfer() ? transaction.maxSets(templates) : 1;
        if (sets < 1 || !transaction.apply(templates, sets)) {
            transaction.rollback();
            return;
        }
        if (!craftingRecipe.matches(CraftingInput.of(3, 3, currentGrid(menu)), player.serverLevel())) {
            transaction.rollback();
            return;
        }
        menu.broadcastChanges();
    }

    private static List<ItemStack> currentGrid(CraftingMenu menu) {
        List<ItemStack> result = new ArrayList<>(9);
        for (int slot = 1; slot <= 9; slot++) {
            result.add(menu.getSlot(slot).getItem().copy());
        }
        return result;
    }

    private static final class Transaction {
        private final CraftingMenu menu;
        private final List<InventoryHandler> backpacks;
        private final List<ItemStack> menuSnapshot = new ArrayList<>(45);
        private final List<List<ItemStack>> backpackSnapshots = new ArrayList<>();

        private Transaction(CraftingMenu menu, List<InventoryHandler> backpacks) {
            this.menu = menu;
            this.backpacks = backpacks;
            for (int slot = 1; slot <= 45; slot++) {
                menuSnapshot.add(menu.getSlot(slot).getItem().copy());
            }
            for (InventoryHandler handler : backpacks) {
                List<ItemStack> snapshot = new ArrayList<>(handler.getSlots());
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    snapshot.add(handler.getStackInSlot(slot).copy());
                }
                backpackSnapshots.add(snapshot);
            }
        }

        private int maxSets(List<ItemStack> templates) {
            int maximum = 64;
            List<ItemStack> types = new ArrayList<>();
            List<Integer> needed = new ArrayList<>();
            for (ItemStack template : templates) {
                if (template.isEmpty()) {
                    continue;
                }
                int type = findType(types, template);
                if (type < 0) {
                    types.add(template);
                    needed.add(1);
                } else {
                    needed.set(type, needed.get(type) + 1);
                }
                maximum = Math.min(maximum, template.getMaxStackSize());
            }
            for (int type = 0; type < types.size(); type++) {
                maximum = Math.min(maximum, count(types.get(type)) / needed.get(type));
            }
            return maximum;
        }

        private int count(ItemStack template) {
            int count = 0;
            for (ItemStack stack : menuSnapshot) {
                if (same(stack, template)) {
                    count += stack.getCount();
                }
            }
            for (List<ItemStack> snapshot : backpackSnapshots) {
                for (ItemStack stack : snapshot) {
                    if (same(stack, template)) {
                        count += stack.getCount();
                    }
                }
            }
            return count;
        }

        private boolean apply(List<ItemStack> templates, int sets) {
            List<ItemStack> oldGrid = new ArrayList<>(9);
            for (int slot = 1; slot <= 9; slot++) {
                Slot gridSlot = menu.getSlot(slot);
                oldGrid.add(gridSlot.getItem().copy());
                gridSlot.set(ItemStack.EMPTY);
            }

            for (int index = 0; index < 9; index++) {
                ItemStack template = templates.get(index);
                if (template.isEmpty()) {
                    continue;
                }
                ItemStack target = template.copyWithCount(sets);
                int remaining = takeFromLoose(oldGrid, target, sets);
                remaining = takeFromMenu(target, remaining);
                remaining = takeFromBackpacks(target, remaining);
                if (remaining != 0 || !menu.getSlot(index + 1).mayPlace(target)) {
                    return false;
                }
                menu.getSlot(index + 1).set(target);
            }

            for (ItemStack stack : oldGrid) {
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack remainder = insertIntoMenu(stack.copy());
                remainder = insertIntoBackpacks(remainder);
                if (!remainder.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private int takeFromLoose(List<ItemStack> loose, ItemStack template, int amount) {
            int remaining = amount;
            for (ItemStack stack : loose) {
                if (!same(stack, template)) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                if (remaining == 0) {
                    break;
                }
            }
            return remaining;
        }

        private int takeFromMenu(ItemStack template, int amount) {
            int remaining = amount;
            for (int slot = 10; slot <= 45 && remaining > 0; slot++) {
                Slot source = menu.getSlot(slot);
                if (!same(source.getItem(), template)) {
                    continue;
                }
                ItemStack removed = source.remove(remaining);
                remaining -= removed.getCount();
            }
            return remaining;
        }

        private int takeFromBackpacks(ItemStack template, int amount) {
            int remaining = amount;
            for (InventoryHandler handler : backpacks) {
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    if (same(handler.getStackInSlot(slot), template)) {
                        remaining -= handler.extractItem(slot, remaining, false).getCount();
                    }
                }
            }
            return remaining;
        }

        private ItemStack insertIntoMenu(ItemStack stack) {
            ItemStack remaining = stack;
            for (int slot = 10; slot <= 45 && !remaining.isEmpty(); slot++) {
                Slot target = menu.getSlot(slot);
                ItemStack present = target.getItem();
                if (!present.isEmpty() && same(present, remaining) && target.mayPlace(remaining)) {
                    int moved = Math.min(remaining.getCount(), Math.min(target.getMaxStackSize(remaining), remaining.getMaxStackSize()) - present.getCount());
                    if (moved > 0) {
                        present.grow(moved);
                        target.setChanged();
                        remaining.shrink(moved);
                    }
                }
            }
            for (int slot = 10; slot <= 45 && !remaining.isEmpty(); slot++) {
                Slot target = menu.getSlot(slot);
                if (target.getItem().isEmpty() && target.mayPlace(remaining)) {
                    int moved = Math.min(remaining.getCount(), target.getMaxStackSize(remaining));
                    target.set(remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                }
            }
            return remaining;
        }

        private ItemStack insertIntoBackpacks(ItemStack stack) {
            ItemStack remaining = stack;
            for (InventoryHandler handler : backpacks) {
                for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                    remaining = handler.insertItem(slot, remaining, false);
                }
            }
            return remaining;
        }

        private void rollback() {
            for (int slot = 1; slot <= 45; slot++) {
                menu.getSlot(slot).set(menuSnapshot.get(slot - 1).copy());
            }
            for (int handlerIndex = 0; handlerIndex < backpacks.size(); handlerIndex++) {
                InventoryHandler handler = backpacks.get(handlerIndex);
                List<ItemStack> snapshot = backpackSnapshots.get(handlerIndex);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    handler.setStackInSlot(slot, snapshot.get(slot).copy());
                }
            }
            menu.broadcastChanges();
        }

        private static int findType(List<ItemStack> types, ItemStack stack) {
            for (int i = 0; i < types.size(); i++) {
                if (same(types.get(i), stack)) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean same(ItemStack first, ItemStack second) {
            return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
        }
    }
}
