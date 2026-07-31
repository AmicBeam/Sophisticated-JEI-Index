package com.sbjeiindex.emi;

import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import java.util.ArrayList;
import java.util.List;

public final class EmiBackpackSlots {
    private EmiBackpackSlots() {
    }

    public static List<Slot> create(Player player) {
        List<IndexedBackpackHandler> indexedBackpackHandlers = BackpackHelper.getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        List<IItemHandlerModifiable> backpackHandlers = indexedBackpackHandlers.stream().map(IndexedBackpackHandler::handler).toList();
        if (backpackHandlers.isEmpty()) {
            return List.of();
        }

        int totalSlots = 0;
        for (IItemHandlerModifiable handler : backpackHandlers) {
            totalSlots += handler.getSlots();
        }

        List<Slot> slots = new ArrayList<>(totalSlots);
        for (int backpackIndex = 0; backpackIndex < backpackHandlers.size(); backpackIndex++) {
            IndexedBackpackHandler indexedHandler = indexedBackpackHandlers.get(backpackIndex);
            IItemHandlerModifiable backpackHandler = indexedHandler.handler();
            int baseOffset = EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET + indexedHandler.index() * EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
            OffsetItemHandlerModifiable offsetHandler = new OffsetItemHandlerModifiable(backpackHandler, baseOffset);

            for (int i = 0; i < backpackHandler.getSlots(); i++) {
                int slotId = baseOffset + i;
                Slot slot = new SlotItemHandler(offsetHandler, slotId, 0, 0);
                slot.index = slotId;
                slots.add(slot);
            }
        }

        return slots;
    }
}
