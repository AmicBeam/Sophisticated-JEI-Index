package com.sbjeiindex.jei;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves the virtual backpack slot ids carried by JEI's legacy and counted packets. */
public final class JeiPacketTransferProcessor {
    private JeiPacketTransferProcessor() {
    }

    public static void process(
        ServerPlayer player,
        List<TransferOperation> transferOperations,
        List<Integer> craftingSlotIds,
        List<Integer> inventorySlotIds,
        boolean maxTransfer,
        boolean requireCompleteSets
    ) {
        AbstractContainerMenu container = player.containerMenu;

        Map<Integer, IItemHandlerModifiable> backpackHandlers = new HashMap<>();
        for (IndexedBackpackHandler indexed : BackpackHelper.getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(player)) {
            backpackHandlers.put(indexed.index(), indexed.handler());
        }
        Map<Integer, OffsetItemHandlerModifiable> offsetHandlers = new HashMap<>();
        Map<Integer, Slot> extraSlots = new HashMap<>();

        List<Slot> craftingSlots = resolveSlots(container, craftingSlotIds, backpackHandlers, offsetHandlers, extraSlots);
        List<Slot> inventorySlots = resolveSlots(container, inventorySlotIds, backpackHandlers, offsetHandlers, extraSlots);
        if (craftingSlots == null || inventorySlots == null) {
            return;
        }

        JeiSlotResolver.set(extraSlots);
        try {
            BasicRecipeTransferHandlerServer.setItems(
                player,
                transferOperations,
                craftingSlots,
                inventorySlots,
                maxTransfer,
                requireCompleteSets
            );
        } finally {
            JeiSlotResolver.clear();
        }
    }

    private static List<Slot> resolveSlots(
        AbstractContainerMenu container,
        List<Integer> slotIds,
        Map<Integer, IItemHandlerModifiable> backpackHandlers,
        Map<Integer, OffsetItemHandlerModifiable> offsetHandlers,
        Map<Integer, Slot> extraSlots
    ) {
        List<Slot> slots = new ArrayList<>(slotIds.size());
        for (int slotId : slotIds) {
            Slot resolved = resolveSlot(container, slotId, backpackHandlers, offsetHandlers, extraSlots);
            if (resolved == null) {
                return null;
            }
            slots.add(resolved);
        }
        return slots;
    }

    private static Slot resolveSlot(
        AbstractContainerMenu container,
        int slotIndex,
        Map<Integer, IItemHandlerModifiable> backpackHandlers,
        Map<Integer, OffsetItemHandlerModifiable> offsetHandlers,
        Map<Integer, Slot> extraSlots
    ) {
        if (slotIndex < JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET) {
            if (slotIndex < 0 || slotIndex >= container.slots.size()) {
                return null;
            }
            return container.getSlot(slotIndex);
        }

        int encoded = slotIndex - JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET;
        int stride = JeiTransferConstants.getBackpackSlotIdStride();
        if (stride <= 0) {
            return null;
        }
        int backpackIndex = encoded / stride;
        int innerSlot = encoded % stride;
        IItemHandlerModifiable handler = backpackHandlers.get(backpackIndex);
        if (handler == null || innerSlot < 0 || innerSlot >= handler.getSlots()) {
            return null;
        }

        OffsetItemHandlerModifiable offsetHandler = offsetHandlers.computeIfAbsent(backpackIndex, ignored -> {
            int baseOffset = JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * stride;
            return new OffsetItemHandlerModifiable(handler, baseOffset);
        });
        Slot slot = new BackpackTransferSlot(offsetHandler, slotIndex, 0, 0);
        slot.index = slotIndex;
        extraSlots.put(slotIndex, slot);
        return slot;
    }
}
