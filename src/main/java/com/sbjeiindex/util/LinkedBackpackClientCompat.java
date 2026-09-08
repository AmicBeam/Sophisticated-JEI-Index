package com.sbjeiindex.util;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Optional bridge for the linked-storage client snapshot API added in Sophisticated Backpacks 3.26. */
public final class LinkedBackpackClientCompat {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long REQUEST_INTERVAL_NANOS = 2_000_000_000L;
    private static final Map<UUID, Long> LAST_REQUESTS = new HashMap<>();
    @Nullable
    private static final Support SUPPORT = Support.find();

    private LinkedBackpackClientCompat() {}

    public static Resolution resolveOrRequest(Level level, ItemStack stack) {
        if (SUPPORT == null) {
            return Resolution.NOT_LINKED;
        }

        try {
            Object endpointData = getEndpointData(stack, SUPPORT.endpointComponent());
            if (endpointData == null) {
                return Resolution.NOT_LINKED;
            }

            UUID groupId = (UUID) SUPPORT.groupId().invoke(endpointData);
            Optional<?> resolved = (Optional<?>) SUPPORT.resolve().invoke(null, level, stack);
            requestSnapshot(groupId);
            if (resolved.isPresent()) {
                return new Resolution(true, (IBackpackWrapper) resolved.get());
            }

            return Resolution.LINKED_PENDING;
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            LOGGER.error("Unable to resolve linked Sophisticated Backpack contents", e);
            return Resolution.LINKED_PENDING;
        }
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getEndpointData(ItemStack stack, Supplier<?> componentSupplier) {
        return stack.get((Supplier<? extends DataComponentType<?>>) (Supplier) componentSupplier);
    }

    private static void requestSnapshot(UUID groupId) throws InvocationTargetException, IllegalAccessException {
        long now = System.nanoTime();
        Long lastRequest = LAST_REQUESTS.get(groupId);
        if (lastRequest != null && now - lastRequest < REQUEST_INTERVAL_NANOS) {
            return;
        }

        long knownRevision = -1;
        Optional<?> revision = (Optional<?>) SUPPORT.getRevision().invoke(null, groupId);
        if (revision.isPresent()) {
            knownRevision = ((Number) revision.get()).longValue();
        }

        try {
            CustomPacketPayload payload = (CustomPacketPayload) SUPPORT.requestConstructor().newInstance(groupId, knownRevision);
            PacketDistributor.sendToServer(payload);
            LAST_REQUESTS.put(groupId, now);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to construct linked backpack contents request", e);
        }
    }

    public record Resolution(boolean linked, @Nullable IBackpackWrapper wrapper) {
        private static final Resolution NOT_LINKED = new Resolution(false, null);
        private static final Resolution LINKED_PENDING = new Resolution(true, null);
    }

    private record Support(
        Supplier<?> endpointComponent,
        Method groupId,
        Method resolve,
        Method getRevision,
        Constructor<?> requestConstructor
    ) {
        @Nullable
        private static Support find() {
            try {
                Class<?> componentsClass = Class.forName("net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents");
                Field endpointField = componentsClass.getField("LINKED_STORAGE_ENDPOINT");
                Supplier<?> endpointComponent = (Supplier<?>) endpointField.get(null);

                Class<?> endpointDataClass = Class.forName("net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData");
                Method groupId = endpointDataClass.getMethod("groupId");

                Class<?> resolverClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver");
                Method resolve = resolverClass.getMethod("resolve", Level.class, ItemStack.class);

                Class<?> clientContentsClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.ClientLinkedStorageBackpackContents");
                Method getRevision = clientContentsClass.getMethod("getRevision", UUID.class);

                Class<?> requestClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.network.RequestLinkedStorageBackpackContentsPayload");
                Constructor<?> requestConstructor = requestClass.getConstructor(UUID.class, long.class);
                return new Support(endpointComponent, groupId, resolve, getRevision, requestConstructor);
            } catch (ClassNotFoundException | NoSuchFieldException e) {
                return null;
            } catch (ReflectiveOperationException | ClassCastException e) {
                LOGGER.error("Sophisticated Backpacks linked-storage API has an incompatible shape", e);
                return null;
            }
        }
    }
}
