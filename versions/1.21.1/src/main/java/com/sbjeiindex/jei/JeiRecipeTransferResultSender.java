package com.sbjeiindex.jei;

import mezz.jei.common.network.ServerPacketContext;
import mezz.jei.common.network.packets.PlayToClientPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

/** Sends JEI 19.53 transfer acknowledgements without linking older JEI runtimes to the new result packet. */
public final class JeiRecipeTransferResultSender {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private static final Constructor<?> RESULT_PACKET = findResultPacket();

    private JeiRecipeTransferResultSender() {}

    public static void send(ServerPacketContext context, int transferId, boolean success) {
        if (RESULT_PACKET == null) {
            LOGGER.error("JEI does not expose recipe transfer result packets");
            return;
        }
        try {
            Object result = RESULT_PACKET.newInstance(transferId, success);
            context.connection().sendPacketToClient((PlayToClientPacket) result, context.player());
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.error("Unable to send JEI recipe transfer result", e);
        }
    }

    @Nullable
    private static Constructor<?> findResultPacket() {
        try {
            Class<?> packetClass = Class.forName("mezz.jei.common.network.packets.PacketRecipeTransferResult");
            return packetClass.getConstructor(int.class, boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }
}
