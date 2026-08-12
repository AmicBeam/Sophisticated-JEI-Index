package com.sbjeiindex.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModPayloads {
    private ModPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1.2.1").playToServer(
            VanillaRecipeTransferPayload.TYPE,
            VanillaRecipeTransferPayload.STREAM_CODEC,
            VanillaRecipeTransferPayload::handle
        );
    }
}
