package com.sbjeiindex.emi;

import com.sbjeiindex.jei.BackpackTransferSlot;
import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class EmiServerSlotResolver {
    private EmiServerSlotResolver() {}

    public static Slot resolve(Player player, int slotId) {
        if (slotId < EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET) {
            return null;
        }
        int encoded = slotId - EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET;
        int backpackIndex = encoded / EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
        int innerSlot = encoded % EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
        for (IndexedBackpackHandler indexed : BackpackHelper.getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(player)) {
            if (indexed.index() != backpackIndex || innerSlot < 0 || innerSlot >= indexed.handler().getSlots()) {
                continue;
            }
            int offset = EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
            OffsetItemHandlerModifiable handler = new OffsetItemHandlerModifiable(indexed.handler(), offset);
            Slot slot = new BackpackTransferSlot(handler, slotId, 0, 0);
            slot.index = slotId;
            return slot;
        }
        return null;
    }
}
