package com.sbjeiindex.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.sbjeiindex.SBJEIIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SBJEIIndex.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        LOGGER.info("Registering network packets");
        int id = 0;
        CHANNEL.registerMessage(id++,
                TransferRecipePacket.class,
                TransferRecipePacket::encode,
                TransferRecipePacket::decode,
                TransferRecipePacket::handle
        );
        LOGGER.info("Network packets registered successfully");
    }
}