package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiPacketTransferProcessor;
import com.sbjeiindex.jei.JeiRecipeTransferResultSender;
import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.transfer.TransferOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.common.network.packets.PacketRecipeTransferWithResult", remap = false)
public class PacketRecipeTransferWithResultMixin {
    @Shadow(remap = false) @Final private List<TransferOperation> transferOperations;
    @Shadow(remap = false) @Final private List<Integer> craftingSlots;
    @Shadow(remap = false) @Final private List<Integer> inventorySlots;
    @Shadow(remap = false) @Final private boolean maxTransfer;
    @Shadow(remap = false) @Final private boolean requireCompleteSets;
    @Shadow(remap = false) @Final private int transferId;

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_process(ServerPacketContext context, CallbackInfo ci) {
        boolean success = JeiPacketTransferProcessor.processWithResult(
            context.player(), transferOperations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets
        );
        JeiRecipeTransferResultSender.send(context, transferId, success);
        ci.cancel();
    }
}
