package com.sbjeiindex.jei;

import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.network.packets.PacketJei;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/** Keeps the counted Forge protocol on both sides of JEI's result-packet change. */
public final class JeiRecipeTransferPacketCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Constructor<?> LEGACY = findPacket(false,
        "mezz.jei.common.network.packets.PacketRecipeTransferCounted",
        "mezz.jei.common.network.packets.legacy.PacketRecipeTransferCounted");
    private static final Constructor<?> WITH_RESULT = findPacket(true,
        "mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult");
    private static final ResultSupport RESULT_SUPPORT = ResultSupport.find();

    private JeiRecipeTransferPacketCompat() {}

    public static boolean send(IConnectionToServer connection, List<TransferOperation> operations,
        List<Slot> craftingSlots, List<Slot> inventorySlots, boolean maxTransfer,
        boolean requireCompleteSets, boolean preferCounted, @Nullable Object context) {
        try {
            boolean withResult = context != null && WITH_RESULT != null && RESULT_SUPPORT != null
                && (boolean) RESULT_SUPPORT.supported().invoke(connection);
            Constructor<?> factory = withResult ? WITH_RESULT : LEGACY;
            if (factory == null) {
                LOGGER.error("JEI does not expose a compatible counted transfer packet");
                return false;
            }
            Object packet;
            if (withResult) {
                int id = ((Number) RESULT_SUPPORT.transferId().invoke(context)).intValue();
                packet = factory.newInstance(operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets, id);
                RESULT_SUPPORT.register().invoke(null, context);
            } else {
                packet = factory.newInstance(operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets);
            }
            connection.sendPacketToServer((PacketJei) packet);
            return true;
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.error("Unable to send JEI recipe transfer", e);
            return false;
        }
    }

    @Nullable
    private static Constructor<?> findPacket(boolean result, String... names) {
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name);
                return result
                    ? type.getConstructor(Collection.class, Collection.class, Collection.class, boolean.class, boolean.class, int.class)
                    : type.getConstructor(Collection.class, Collection.class, Collection.class, boolean.class, boolean.class);
            } catch (ClassNotFoundException e) {
                // The old packet moved to the legacy package in newer JEI.
            } catch (NoSuchMethodException e) {
                LOGGER.error("Incompatible JEI transfer packet: {}", name, e);
            }
        }
        return null;
    }

    private record ResultSupport(Method supported, Method transferId, Method register) {
        @Nullable
        private static ResultSupport find() {
            try {
                Class<?> context = Class.forName("mezz.jei.api.recipe.transfer.IRecipeTransferContext");
                Class<?> result = Class.forName("mezz.jei.common.network.packets.PacketRecipeTransferResult");
                return new ResultSupport(IConnectionToServer.class.getMethod("supportsRecipeTransferResults"),
                    context.getMethod("getTransferId"), result.getMethod("registerPendingRecipeTransfer", context));
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                return null;
            }
        }
    }
}
