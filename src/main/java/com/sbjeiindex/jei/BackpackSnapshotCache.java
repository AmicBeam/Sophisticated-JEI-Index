package com.sbjeiindex.jei;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class BackpackSnapshotCache {
    private static final Map<AbstractContainerMenu, BackpackSnapshot> BY_CONTAINER = new WeakHashMap<>();

    private BackpackSnapshotCache() {
    }

    public static BackpackSnapshot getOrCreate(AbstractContainerMenu container, List<IItemHandlerModifiable> handlers) {
        List<IndexedBackpackHandler> indexedHandlers = new ArrayList<>(handlers.size());
        for (int i = 0; i < handlers.size(); i++) {
            indexedHandlers.add(new IndexedBackpackHandler(i, handlers.get(i)));
        }
        return getOrCreateIndexed(container, indexedHandlers);
    }

    public static BackpackSnapshot getOrCreateIndexed(AbstractContainerMenu container, List<IndexedBackpackHandler> handlers) {
        BackpackSnapshot existing = BY_CONTAINER.get(container);
        if (existing != null && sameHandlers(existing.handlers, handlers)) {
            return existing;
        }
        BackpackSnapshot snapshot = BackpackSnapshot.build(handlers);
        BY_CONTAINER.put(container, snapshot);
        return snapshot;
    }

    private static boolean sameHandlers(List<IndexedBackpackHandler> a, List<IndexedBackpackHandler> b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).index() != b.get(i).index() || a.get(i).handler() != b.get(i).handler()) {
                return false;
            }
        }
        return true;
    }

    public static final class BackpackSnapshot {
        private final List<IndexedBackpackHandler> handlers;
        private final List<Slot> backpackSlots;
        private final Map<Integer, Slot> extraSlots;
        private final Map<Integer, ItemStack> nonEmptyStacks;
        private final int totalBackpackSlots;
        private final int emptyBackpackSlots;

        private BackpackSnapshot(
            List<IndexedBackpackHandler> handlers,
            List<Slot> backpackSlots,
            Map<Integer, Slot> extraSlots,
            Map<Integer, ItemStack> nonEmptyStacks,
            int totalBackpackSlots,
            int emptyBackpackSlots
        ) {
            this.handlers = handlers;
            this.backpackSlots = backpackSlots;
            this.extraSlots = extraSlots;
            this.nonEmptyStacks = nonEmptyStacks;
            this.totalBackpackSlots = totalBackpackSlots;
            this.emptyBackpackSlots = emptyBackpackSlots;
        }

        public List<Slot> backpackSlots() {
            return backpackSlots;
        }

        public Map<Integer, Slot> extraSlots() {
            return extraSlots;
        }

        public Map<Integer, ItemStack> nonEmptyStacks() {
            return nonEmptyStacks;
        }

        public int totalBackpackSlots() {
            return totalBackpackSlots;
        }

        public int emptyBackpackSlots() {
            return emptyBackpackSlots;
        }

        private static BackpackSnapshot build(List<IndexedBackpackHandler> handlers) {
            int totalSlots = 0;
            for (IndexedBackpackHandler handler : handlers) {
                totalSlots += handler.handler().getSlots();
            }

            List<Slot> backpackSlots = new ArrayList<>(totalSlots);
            Map<Integer, Slot> extraSlots = new HashMap<>(Math.max(16, totalSlots * 2));
            Map<Integer, ItemStack> nonEmptyStacks = new HashMap<>();

            int nonEmptyCount = 0;
            for (int backpackIndex = 0; backpackIndex < handlers.size(); backpackIndex++) {
                IndexedBackpackHandler indexedHandler = handlers.get(backpackIndex);
                IItemHandlerModifiable backpackHandler = indexedHandler.handler();
                int baseOffset = JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET + indexedHandler.index() * JeiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
                OffsetItemHandlerModifiable offsetHandler = new OffsetItemHandlerModifiable(backpackHandler, baseOffset);

                int slots = backpackHandler.getSlots();
                for (int i = 0; i < slots; i++) {
                    int slotId = baseOffset + i;
                    Slot slot = new BackpackTransferSlot(offsetHandler, slotId, 0, 0);
                    slot.index = slotId;
                    backpackSlots.add(slot);
                    extraSlots.put(slotId, slot);

                    ItemStack stack = backpackHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        nonEmptyStacks.put(slotId, stack.copy());
                        nonEmptyCount++;
                    }
                }
            }

            int emptySlots = totalSlots - nonEmptyCount;
            return new BackpackSnapshot(
                List.copyOf(handlers),
                List.copyOf(backpackSlots),
                Map.copyOf(extraSlots),
                Map.copyOf(nonEmptyStacks),
                totalSlots,
                emptySlots
            );
        }
    }
}
