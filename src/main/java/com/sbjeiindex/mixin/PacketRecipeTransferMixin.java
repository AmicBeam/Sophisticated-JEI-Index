package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiPacketTransferProcessor;
import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.transfer.TransferOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.common.network.packets.PacketRecipeTransfer", remap = false)
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
        JeiPacketTransferProcessor.process(
            context.player(), transferOperations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets
        );
        ci.cancel();
    }
}
