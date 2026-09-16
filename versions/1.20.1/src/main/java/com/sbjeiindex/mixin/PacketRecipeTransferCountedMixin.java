package com.sbjeiindex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.sbjeiindex.jei.JeiForgePacketReader;
import mezz.jei.common.network.ServerPacketData;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets = "mezz.jei.common.network.packets.PacketRecipeTransferCounted", remap = false)
public class PacketRecipeTransferCountedMixin {
    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbjeiindex_read(ServerPacketData data, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.setReturnValue(JeiForgePacketReader.read(data, true, false));
    }
}
