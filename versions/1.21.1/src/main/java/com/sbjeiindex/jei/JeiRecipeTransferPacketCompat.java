package com.sbjeiindex.jei;

import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.network.packets.PlayToServerPacket;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/** Bridges JEI 19.44 packet names and the legacy package introduced in JEI 19.53. */
public final class JeiRecipeTransferPacketCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final PacketFactory BASIC = findFactory(
        "mezz.jei.common.network.packets.PacketRecipeTransfer",
        "mezz.jei.common.network.packets.legacy.PacketRecipeTransfer"
    );
    private static final PacketFactory COUNTED = findFactory(
        false,
        "mezz.jei.common.network.packets.PacketRecipeTransferCounted",
        "mezz.jei.common.network.packets.legacy.PacketRecipeTransferCounted"
    );
    private static final PacketFactory BASIC_WITH_RESULT = findFactory(
        true,
        "mezz.jei.common.network.packets.PacketRecipeTransferWithResult"
    );
    private static final PacketFactory COUNTED_WITH_RESULT = findFactory(
        true,
        "mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult"
    );
    @Nullable
    private static final ResultSupport RESULT_SUPPORT = ResultSupport.find();

    private JeiRecipeTransferPacketCompat() {}

    public static boolean send(
        IConnectionToServer connection,
        List<TransferOperation> operations,
        List<Slot> craftingSlots,
        List<Slot> inventorySlots,
        boolean maxTransfer,
        boolean requireCompleteSets,
        boolean preferCounted,
        @Nullable Object transferContext
    ) {
        PacketFactory resultFactory = preferCounted ? COUNTED_WITH_RESULT : BASIC_WITH_RESULT;
        boolean withResult = transferContext != null && RESULT_SUPPORT != null && canSend(connection, resultFactory);
        PacketFactory factory = withResult
            ? resultFactory
            : (preferCounted && canSend(connection, COUNTED) ? COUNTED : BASIC);
        if (!canSend(connection, factory)) {
            LOGGER.error("JEI does not expose a compatible recipe transfer packet");
            return false;
        }

        try {
            Object packet;
            if (withResult) {
                int transferId = ((Number) RESULT_SUPPORT.getTransferId().invoke(transferContext)).intValue();
                packet = factory.fromSlots().invoke(
                    null, operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets, transferId
                );
                RESULT_SUPPORT.registerPending().invoke(null, transferContext);
            } else {
                packet = factory.fromSlots().invoke(
                    null, operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets
                );
            }
            connection.sendPacketToServer((PlayToServerPacket) packet);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            LOGGER.error("Unable to create or send JEI's recipe transfer packet", e);
            return false;
        }
    }

    private static boolean canSend(IConnectionToServer connection, @Nullable PacketFactory factory) {
        return factory != null && connection.canSendPacket(factory.type());
    }

    @Nullable
    private static PacketFactory findFactory(String... classNames) {
        return findFactory(false, classNames);
    }

    @Nullable
    private static PacketFactory findFactory(boolean withResult, String... classNames) {
        for (String className : classNames) {
            try {
                Class<?> packetClass = Class.forName(className);
                Method fromSlots = withResult
                    ? packetClass.getMethod(
                        "fromSlots", List.class, List.class, List.class, boolean.class, boolean.class, int.class
                    )
                    : packetClass.getMethod(
                        "fromSlots", List.class, List.class, List.class, boolean.class, boolean.class
                    );
                Field typeField = packetClass.getField("TYPE");
                CustomPacketPayload.Type<?> type = (CustomPacketPayload.Type<?>) typeField.get(null);
                return new PacketFactory(fromSlots, type);
            } catch (ClassNotFoundException e) {
                // Try the package used by the other supported JEI version.
            } catch (ReflectiveOperationException | ClassCastException e) {
                LOGGER.error("JEI recipe transfer packet {} has an incompatible API", className, e);
            }
        }
        return null;
    }

    private record PacketFactory(Method fromSlots, CustomPacketPayload.Type<?> type) {}

    private record ResultSupport(Method getTransferId, Method registerPending) {
        @Nullable
        private static ResultSupport find() {
            try {
                Class<?> contextClass = Class.forName("mezz.jei.api.recipe.transfer.IRecipeTransferContext");
                Method getTransferId = contextClass.getMethod("getTransferId");
                Class<?> resultPacketClass = Class.forName("mezz.jei.common.network.packets.PacketRecipeTransferResult");
                Method registerPending = resultPacketClass.getMethod("registerPendingRecipeTransfer", contextClass);
                return new ResultSupport(getTransferId, registerPending);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                return null;
            }
        }
    }
}
