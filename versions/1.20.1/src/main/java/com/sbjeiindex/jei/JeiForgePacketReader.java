package com.sbjeiindex.jei;

import mezz.jei.common.network.ServerPacketData;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Decodes each Forge JEI wire format, then resolves backpack slots on the server thread. */
public final class JeiForgePacketReader {
    private JeiForgePacketReader() {}

    public static CompletableFuture<Void> read(ServerPacketData data, boolean counted, boolean withResult) {
        var context = data.context();
        var player = context.player();
        FriendlyByteBuf buf = data.buf();
        int size = buf.readVarInt();
        if (size < 0 || size > buf.readableBytes()) {
            throw new IllegalArgumentException("Invalid JEI transfer operation count");
        }
        List<TransferOperation> operations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            operations.add(counted
                ? TransferOperation.readCountedPacketData(buf, player.containerMenu)
                : TransferOperation.readPacketData(buf, player.containerMenu));
        }
        List<Integer> crafting = readSlots(buf);
        List<Integer> inventory = readSlots(buf);
        boolean maxTransfer = buf.readBoolean();
        boolean completeSets = buf.readBoolean();
        int id = withResult ? buf.readVarInt() : 0;
        return player.server.submit(() -> {
            if (withResult) {
                boolean success = JeiPacketTransferProcessor.processWithResult(
                    player, operations, crafting, inventory, maxTransfer, completeSets);
                JeiRecipeTransferResultSender.send(context, id, success);
            } else {
                JeiPacketTransferProcessor.process(player, operations, crafting, inventory, maxTransfer, completeSets);
            }
        });
    }

    private static List<Integer> readSlots(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > buf.readableBytes()) {
            throw new IllegalArgumentException("Invalid JEI transfer slot count");
        }
        List<Integer> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(buf.readVarInt());
        }
        return slots;
    }
}
