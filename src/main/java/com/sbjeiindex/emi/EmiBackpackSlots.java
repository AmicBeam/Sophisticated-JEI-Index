package com.sbjeiindex.emi;

import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public final class EmiBackpackSlots {
    private EmiBackpackSlots() {
    }

    public static List<Slot> create(Player player) {
        return create(player, null);
    }

    public static List<Slot> create(Player player, IItemHandlerModifiable excludedHandler) {
        List<IItemHandlerModifiable> backpackHandlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (backpackHandlers.isEmpty()) {
            return List.of();
        }

        int totalSlots = 0;
        for (IItemHandlerModifiable handler : backpackHandlers) {
            if (handler == excludedHandler) {
                continue;
            }
            totalSlots += handler.getSlots();
        }

        List<Slot> slots = new ArrayList<>(totalSlots);
        for (int backpackIndex = 0; backpackIndex < backpackHandlers.size(); backpackIndex++) {
            IItemHandlerModifiable backpackHandler = backpackHandlers.get(backpackIndex);
            if (backpackHandler == excludedHandler) {
                continue;
            }
            int baseOffset = EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
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
