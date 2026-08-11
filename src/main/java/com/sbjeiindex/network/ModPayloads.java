package com.sbjeiindex.network;

import com.sbjeiindex.SBJEIIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModPayloads {
    private static final String VERSION = "1.2.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SBJEIIndex.MOD_ID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private ModPayloads() {}

    public static void register() {
        CHANNEL.registerMessage(
            0,
            VanillaRecipeTransferPayload.class,
            VanillaRecipeTransferPayload::encode,
            VanillaRecipeTransferPayload::decode,
            VanillaRecipeTransferPayload::handle
        );
    }
}
