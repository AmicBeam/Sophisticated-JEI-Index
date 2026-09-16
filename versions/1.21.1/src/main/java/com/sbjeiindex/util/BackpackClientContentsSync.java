package com.sbjeiindex.util;

import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsPayload;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps the client-side UUID-backed backpack storage fresh while JEI is inspecting it. */
public final class BackpackClientContentsSync {
    private static final long REQUEST_INTERVAL_NANOS = 2_000_000_000L;
    private static final Map<UUID, Long> LAST_REQUESTS = new HashMap<>();
    private static final Map<UUID, List<WeakReference<IBackpackWrapper>>> WRAPPERS = new HashMap<>();

    private BackpackClientContentsSync() {
    }

    public static void registerAndRequest(IBackpackWrapper wrapper) {
        wrapper.getContentsUuid().ifPresent(uuid -> {
            register(uuid, wrapper);
            long now = System.nanoTime();
            Long lastRequest = LAST_REQUESTS.get(uuid);
            if (lastRequest == null || now - lastRequest >= REQUEST_INTERVAL_NANOS) {
                LAST_REQUESTS.put(uuid, now);
                PacketDistributor.sendToServer(new RequestBackpackInventoryContentsPayload(uuid));
            }
        });
    }

    private static void register(UUID uuid, IBackpackWrapper wrapper) {
        List<WeakReference<IBackpackWrapper>> references = WRAPPERS.computeIfAbsent(uuid, ignored -> new ArrayList<>());
        boolean registered = false;
        for (int i = references.size() - 1; i >= 0; i--) {
            IBackpackWrapper existing = references.get(i).get();
            if (existing == null) {
                references.remove(i);
            } else if (existing == wrapper) {
                registered = true;
            }
        }
        if (!registered) {
            references.add(new WeakReference<>(wrapper));
        }
    }

    public static void contentsUpdated(UUID uuid) {
        List<WeakReference<IBackpackWrapper>> references = WRAPPERS.get(uuid);
        if (references == null) {
            return;
        }
        for (int i = references.size() - 1; i >= 0; i--) {
            IBackpackWrapper wrapper = references.get(i).get();
            if (wrapper == null) {
                references.remove(i);
            } else {
                wrapper.onContentsNbtUpdated();
            }
        }
        if (references.isEmpty()) {
            WRAPPERS.remove(uuid);
            LAST_REQUESTS.remove(uuid);
        }
    }
}
