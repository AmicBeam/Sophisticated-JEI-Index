package com.sbjeiindex.jei;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves the virtual backpack slot ids carried by JEI's legacy and counted packets. */
public final class JeiPacketTransferProcessor {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private static final Method SET_ITEMS_WITH_RESULT = findSetItemsWithResult();

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
        ResolvedTransfer resolved = resolve(player, craftingSlotIds, inventorySlotIds);
        if (resolved == null) {
            return;
        }

        JeiSlotResolver.set(resolved.extraSlots());
        try {
            BasicRecipeTransferHandlerServer.setItems(
                player,
                transferOperations,
                resolved.craftingSlots(),
                resolved.inventorySlots(),
                maxTransfer,
                requireCompleteSets
            );
        } finally {
            JeiSlotResolver.clear();
        }
    }

    public static boolean processWithResult(
        ServerPlayer player,
        List<TransferOperation> transferOperations,
        List<Integer> craftingSlotIds,
        List<Integer> inventorySlotIds,
        boolean maxTransfer,
        boolean requireCompleteSets
    ) {
        ResolvedTransfer resolved = resolve(player, craftingSlotIds, inventorySlotIds);
        if (resolved == null) {
            return false;
        }

        if (SET_ITEMS_WITH_RESULT == null) {
            return false;
        }

        JeiSlotResolver.set(resolved.extraSlots());
        try {
            return (boolean) SET_ITEMS_WITH_RESULT.invoke(
                null, player, transferOperations, resolved.craftingSlots(), resolved.inventorySlots(), maxTransfer, requireCompleteSets
            );
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            LOGGER.error("Unable to run JEI recipe transfer with a result", e);
            return false;
        } finally {
            JeiSlotResolver.clear();
        }
    }

    @Nullable
    private static Method findSetItemsWithResult() {
        try {
            return BasicRecipeTransferHandlerServer.class.getMethod(
                "setItemsWithResult", Player.class, List.class, List.class, List.class, boolean.class, boolean.class
            );
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static ResolvedTransfer resolve(
        ServerPlayer player,
        List<Integer> craftingSlotIds,
        List<Integer> inventorySlotIds
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
            return null;
        }
        return new ResolvedTransfer(craftingSlots, inventorySlots, extraSlots);
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
        int stride = JeiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
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

    private record ResolvedTransfer(List<Slot> craftingSlots, List<Slot> inventorySlots, Map<Integer, Slot> extraSlots) {}
}
