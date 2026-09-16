package com.sbjeiindex.mixin;

import com.sbjeiindex.util.BackpackClientContentsSync;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BackpackContentsPayload.class, remap = false)
public class BackpackContentsPayloadMixin {
    @Inject(method = "handlePayload", at = @At("TAIL"), remap = false)
    private static void sbjeiindex_refreshRegisteredWrapper(
        BackpackContentsPayload payload,
        IPayloadContext context,
        CallbackInfo ci
    ) {
        BackpackClientContentsSync.contentsUpdated(payload.backpackUuid());
    }
}
