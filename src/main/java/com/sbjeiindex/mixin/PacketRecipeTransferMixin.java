package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.BackpackTransferSlot;
import com.sbjeiindex.jei.JeiSlotResolver;
import com.sbjeiindex.jei.JeiTransferConstants;
import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackHelper.IndexedBackpackHandler;
import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.network.packets.PacketRecipeTransfer;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = PacketRecipeTransfer.class, remap = false)
public class PacketRecipeTransferMixin {
    @Shadow(remap = false)
    @Final
    public List<TransferOperation> transferOperations;

    @Shadow(remap = false)
    @Final
    public List<Integer> craftingSlots;

    @Shadow(remap = false)
    @Final
    public List<Integer> inventorySlots;

    @Shadow(remap = false)
    @Final
    private boolean maxTransfer;

    @Shadow(remap = false)
    @Final
    private boolean requireCompleteSets;

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_process(ServerPacketContext context, CallbackInfo ci) {
        ServerPlayer player = context.player();
        AbstractContainerMenu container = player.containerMenu;

        Map<Integer, IItemHandlerModifiable> backpackHandlers = new HashMap<>();
        for (IndexedBackpackHandler indexed : BackpackHelper.getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(player)) {
            backpackHandlers.put(indexed.index(), indexed.handler());
        }
        Map<Integer, OffsetItemHandlerModifiable> offsetHandlers = new HashMap<>();

        List<Slot> craftingSlotsResolved = new ArrayList<>(craftingSlots.size());
        List<Slot> inventorySlotsResolved = new ArrayList<>(inventorySlots.size());
        Map<Integer, Slot> extraSlots = new HashMap<>();
        boolean invalidBackpackSlotIds = false;

        for (int slotIndex : craftingSlots) {
            Slot resolved = resolveSlot(container, slotIndex, backpackHandlers, offsetHandlers, extraSlots);
            if (resolved == null) {
                invalidBackpackSlotIds = true;
                continue;
            }
            craftingSlotsResolved.add(resolved);
        }
        for (int slotIndex : inventorySlots) {
            Slot resolved = resolveSlot(container, slotIndex, backpackHandlers, offsetHandlers, extraSlots);
            if (resolved == null) {
                invalidBackpackSlotIds = true;
                continue;
            }
            inventorySlotsResolved.add(resolved);
        }

        if (invalidBackpackSlotIds) {
            ci.cancel();
            return;
        }

        JeiSlotResolver.set(extraSlots);
        try {
            BasicRecipeTransferHandlerServer.setItems(
                player,
                transferOperations,
                craftingSlotsResolved,
                inventorySlotsResolved,
                maxTransfer,
                requireCompleteSets
            );
        } finally {
            JeiSlotResolver.clear();
        }
        ci.cancel();
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
        if (backpackHandlers.isEmpty()) {
            return null;
        }

        int encoded = slotIndex - JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET;
        int stride = JeiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
        if (stride <= 0) {
            return null;
        }

        int backpackIndex = encoded / stride;
        int innerSlot = encoded % stride;
        IItemHandlerModifiable handler = backpackHandlers.get(backpackIndex);
        if (handler == null || innerSlot < 0 || innerSlot >= handler.getSlots()) {
            return null;
        }

        OffsetItemHandlerModifiable offsetHandler = offsetHandlers.get(backpackIndex);
        if (offsetHandler == null) {
            int baseOffset = JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * stride;
            offsetHandler = new OffsetItemHandlerModifiable(handler, baseOffset);
            offsetHandlers.put(backpackIndex, offsetHandler);
        }
        Slot slot = new BackpackTransferSlot(offsetHandler, slotIndex, 0, 0);
        slot.index = slotIndex;
        extraSlots.put(slotIndex, slot);
        return slot;
    }
}
