package com.sbjeiindex;

import com.sbjeiindex.util.LinkedBackpackClientCompat;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.neoforged.fml.config.ConfigTracker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Development-client-only probe; copy into an isolated project, never the release source set. */
public final class LinkedBackpackCompatProbe {
    public static void run() {
        try {
            // A menu-only client has not received server config values yet.
            // Supply defaults in memory for the synthetic snapshot fixture.
            if (!net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER_SPEC.isLoaded()) {
                ConfigTracker.INSTANCE.loadDefaultServerConfigs();
            }
            Class.forName("mezz.jei.library.transfer.BasicRecipeTransferHandler");
            Class.forName("mezz.jei.common.network.packets.PacketRecipeTransferCountedWithResult");
            Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackContentsPayload");

            Field field = LinkedBackpackClientCompat.class.getDeclaredField("SUPPORT");
            field.setAccessible(true);
            Object support = field.get(null);
            require(support != null, "Linked bridge unavailable");
            Method revision = (Method) accessor(support, "getRevision");
            Constructor<?> request = (Constructor<?>) accessor(support, "requestConstructor");
            Class<?> cache = revision.getDeclaringClass();
            String expected = System.getProperty("sji.test.expectedLinkedApi");
            require(cache.getName().startsWith("net.p3pp3rf1y." + expected + "."), "Wrong API: " + cache);

            UUID group = UUID.randomUUID();
            require(((Optional<?>) revision.invoke(null, group)).isEmpty(), "Unexpected initial snapshot");
            checkRequest(request, group, -1L, expected);

            ItemStack endpoint = new ItemStack(ModItems.BACKPACK.get());
            Class<?> endpointType = Class.forName("net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData");
            Object endpointData = endpointType.getConstructor(UUID.class, UUID.class).newInstance(group, UUID.randomUUID());
            setEndpoint(endpoint, (Supplier<?>) accessor(support, "endpointComponent"), endpointData);

            // No connection/world is present. Suppress only network sending so
            // this probe can exercise real pending/cached resolution in isolation.
            Field requestsField = LinkedBackpackClientCompat.class.getDeclaredField("LAST_REQUESTS");
            requestsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Long> requests = (Map<UUID, Long>) requestsField.get(null);
            requests.put(group, System.nanoTime());
            LinkedBackpackClientCompat.Resolution pending = LinkedBackpackClientCompat.resolveOrRequest(null, endpoint);
            require(pending.linked() && pending.wrapper() == null, "Missing snapshot must remain linked/pending");

            install(cache, group, 7L);
            require(((Optional<?>) revision.invoke(null, group)).orElseThrow().equals(7L), "Wrong revision after snapshot");
            LinkedBackpackClientCompat.Resolution resolved = LinkedBackpackClientCompat.resolveOrRequest(null, endpoint);
            require(resolved.linked() && resolved.wrapper() != null, "Cached linked wrapper not resolved");
            require(resolved.wrapper().getInventoryHandler().getSlots() == 27, "Snapshot inventory size not applied");
            checkRequest(request, group, 7L, expected);

            install(cache, group, 8L);
            require(((Optional<?>) revision.invoke(null, group)).orElseThrow().equals(8L), "Updated revision not visible");
            checkRequest(request, group, 8L, expected);
            requests.remove(group);
            cache.getMethod("clear").invoke(null);
            System.out.println("SJI_LINKED_PROBE PASS api=" + expected + " pending/cached/revision/request/mixins");
            System.exit(0);
        } catch (Throwable failure) {
            System.err.println("SJI_LINKED_PROBE FAIL");
            failure.printStackTrace();
            System.exit(42);
        }
    }

    private static Object accessor(Object record, String name) throws ReflectiveOperationException {
        Method method = record.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(record);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEndpoint(ItemStack stack, Supplier<?> component, Object value) {
        stack.set((DataComponentType) component.get(), value);
    }

    private static void install(Class<?> cache, UUID group, long revision) throws ReflectiveOperationException {
        CompoundTag contents = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("Size", 27);
        contents.put("inventory", inventory);
        if (cache.getName().contains("sophisticatedcore")) {
            cache.getMethod("updateContents", UUID.class, long.class, CompoundTag.class, Component.class,
                int.class, int.class, int.class).invoke(null, group, revision, contents, Component.literal("Probe"), 27, 1, 0);
        } else {
            Class<?> size = Class.forName(cache.getName() + "$StorageSize");
            Object dimensions = size.getConstructor(int.class, int.class).newInstance(27, 1);
            Object accepted = cache.getMethod("installSnapshot", UUID.class, long.class, CompoundTag.class,
                Component.class, size, int.class).invoke(null, group, revision, contents, Component.literal("Probe"), dimensions, 0);
            require(Boolean.TRUE.equals(accepted), "Legacy snapshot rejected");
        }
    }

    private static void checkRequest(Constructor<?> request, UUID group, long revision, String expected) throws ReflectiveOperationException {
        Object payload = request.newInstance(group, revision);
        require(group.equals(payload.getClass().getMethod("groupId").invoke(payload)), "Wrong request group");
        require(Long.valueOf(revision).equals(payload.getClass().getMethod("knownRevision").invoke(payload)), "Wrong request revision");
        require(((CustomPacketPayload) payload).type().id().getNamespace().equals(expected), "Wrong request namespace");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
