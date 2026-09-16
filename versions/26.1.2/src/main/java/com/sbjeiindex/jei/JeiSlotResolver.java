package com.sbjeiindex.jei;

import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.Map;

public final class JeiSlotResolver {
    private static final ThreadLocal<Map<Integer, Slot>> EXTRA_SLOTS = new ThreadLocal<>();

    private JeiSlotResolver() {
    }

    public static void set(@Nullable Map<Integer, Slot> slots) {
        if (slots == null || slots.isEmpty()) {
            EXTRA_SLOTS.remove();
        } else {
            EXTRA_SLOTS.set(slots);
        }
    }

    @Nullable
    public static Slot resolve(int slotId) {
        Map<Integer, Slot> slots = EXTRA_SLOTS.get();
        if (slots == null) {
            return null;
        }
        return slots.get(slotId);
    }

    public static void clear() {
        EXTRA_SLOTS.remove();
    }
}
