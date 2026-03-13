package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiSlotResolver;
import com.sbjeiindex.jei.JeiTransferConstants;
import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.network.ServerPacketData;
import mezz.jei.common.network.packets.PacketRecipeTransfer;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(value = PacketRecipeTransfer.class, remap = false)
public class PacketRecipeTransferMixin {
    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbjeiindex_readPacketData(ServerPacketData data, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        ServerPacketContext context = data.context();
        ServerPlayer player = context.player();
        FriendlyByteBuf buf = data.buf();
        AbstractContainerMenu container = player.containerMenu;

        int transferOperationsSize = buf.readVarInt();
        List<TransferOperation> transferOperations = new ArrayList<>();
        for (int i = 0; i < transferOperationsSize; i++) {
            TransferOperation transferOperation = TransferOperation.readPacketData(buf, container);
            transferOperations.add(transferOperation);
        }

        int craftingSlotsSize = buf.readVarInt();
        List<Slot> craftingSlots = new ArrayList<>();
        for (int i = 0; i < craftingSlotsSize; i++) {
            int slotIndex = buf.readVarInt();
            Slot slot = container.getSlot(slotIndex);
            craftingSlots.add(slot);
        }

        List<IItemHandlerModifiable> backpackHandlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        OffsetItemHandlerModifiable[] offsetHandlers = backpackHandlers.isEmpty() ? null : new OffsetItemHandlerModifiable[backpackHandlers.size()];

        int inventorySlotsSize = buf.readVarInt();
        List<Slot> inventorySlots = new ArrayList<>();
        Map<Integer, Slot> extraSlots = new HashMap<>();
        boolean hasBackpackSlotIds = false;
        boolean invalidBackpackSlotIds = false;
        for (int i = 0; i < inventorySlotsSize; i++) {
            int slotIndex = buf.readVarInt();
            if (slotIndex >= JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET) {
                hasBackpackSlotIds = true;
                if (backpackHandlers.isEmpty()) {
                    invalidBackpackSlotIds = true;
                    continue;
                }

                int encoded = slotIndex - JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET;
                int stride = JeiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
                if (stride <= 0) {
                    invalidBackpackSlotIds = true;
                    continue;
                }

                int backpackIndex = encoded / stride;
                int innerSlot = encoded % stride;
                if (backpackIndex < 0 || backpackIndex >= backpackHandlers.size()) {
                    invalidBackpackSlotIds = true;
                    continue;
                }

                IItemHandlerModifiable handler = backpackHandlers.get(backpackIndex);
                if (handler == null || innerSlot < 0 || innerSlot >= handler.getSlots()) {
                    invalidBackpackSlotIds = true;
                    continue;
                }

                OffsetItemHandlerModifiable offsetHandler = offsetHandlers == null ? null : offsetHandlers[backpackIndex];
                if (offsetHandler == null) {
                    int baseOffset = JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * stride;
                    offsetHandler = new OffsetItemHandlerModifiable(handler, baseOffset);
                    if (offsetHandlers != null) {
                        offsetHandlers[backpackIndex] = offsetHandler;
                    }
                }
                Slot slot = new SlotItemHandler(offsetHandler, slotIndex, 0, 0);
                slot.index = slotIndex;
                inventorySlots.add(slot);
                extraSlots.put(slotIndex, slot);
            } else {
                Slot slot = container.getSlot(slotIndex);
                inventorySlots.add(slot);
            }
        }

        boolean maxTransfer = buf.readBoolean();
        boolean requireCompleteSets = buf.readBoolean();

        if (hasBackpackSlotIds && (backpackHandlers.isEmpty() || invalidBackpackSlotIds)) {
            LOGGER.debug("Ignoring JEI recipe transfer: packet referenced backpack slots but no valid backpack slot mapping was found for {}", player.getGameProfile().getName());
            cir.setReturnValue(CompletableFuture.completedFuture(null));
            return;
        }

        MinecraftServer server = player.server;
        CompletableFuture<Void> future = server.submit(() -> {
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
        });

        cir.setReturnValue(future);
    }
}
