package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiPacketTransferProcessor;
import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult;
import mezz.jei.common.network.packets.PacketRecipeTransferResult;
import mezz.jei.common.transfer.TransferOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PacketRecipeTransferCountedWithResult.class, remap = false)
public class PacketRecipeTransferCountedMixin {
    @Shadow(remap = false) @Final private List<TransferOperation> transferOperations;
    @Shadow(remap = false) @Final private List<Integer> craftingSlots;
    @Shadow(remap = false) @Final private List<Integer> inventorySlots;
    @Shadow(remap = false) @Final private boolean maxTransfer;
    @Shadow(remap = false) @Final private boolean requireCompleteSets;

    @Shadow(remap = false) @Final private int transferId;

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_process(ServerPacketContext context, CallbackInfo ci) {
        boolean success = JeiPacketTransferProcessor.process(
            context.player(), transferOperations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets
        );
        context.connection().sendPacketToClient(new PacketRecipeTransferResult(transferId, success), context.player());
        ci.cancel();
    }
}
