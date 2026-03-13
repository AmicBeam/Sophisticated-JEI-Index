package com.sbjeiindex.util;

import com.sbjeiindex.config.SBJEIIndexConfig;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<IBackpackWrapper, Byte> UPGRADE_REFRESH_STATE = new WeakHashMap<>();

    public static List<IBackpackWrapper> getEquippedBackpacksWithJEIIndexUpgrade(Player player) {
        int maxScanned = SBJEIIndexConfig.maxEnabledBackpacksScanned.get();
        List<IBackpackWrapper> results = new ArrayList<>();
        Set<IBackpackWrapper> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
            IBackpackWrapper wrapper = getBackpackWrapper(backpack);
            if (wrapper == null) {
                return false;
            }
            if (hasJEIIndexUpgrade(wrapper)) {
                if (seen.add(wrapper)) {
                    results.add(wrapper);
                    if (maxScanned > 0 && results.size() >= maxScanned) {
                        return true;
                    }
                }
            }
            return false;
        });
        return results;
    }

    public static List<InventoryHandler> getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        List<InventoryHandler> handlers = new ArrayList<>(wrappers.size());
        Set<InventoryHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IBackpackWrapper wrapper : wrappers) {
            InventoryHandler h = wrapper.getInventoryHandler();
            if (h != null && seen.add(h)) {
                handlers.add(h);
            }
        }
        return handlers;
    }

    public static List<IItemHandlerModifiable> getEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        List<IItemHandlerModifiable> handlers = new ArrayList<>(wrappers.size());
        Set<IItemHandlerModifiable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IBackpackWrapper wrapper : wrappers) {
            IItemHandlerModifiable h = wrapper.getInventoryHandler();
            if (h != null && seen.add(h)) {
                handlers.add(h);
            }
        }
        return handlers;
    }

    @Nullable
    public static IBackpackWrapper getEquippedBackpackWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        if (wrappers.isEmpty()) {
            return null;
        }
        return wrappers.get(0);
    }

    @Nullable
    public static IBackpackWrapper getBackpackWrapper(ItemStack stack) {
        LazyOptional<IBackpackWrapper> cap = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance());
        return cap.resolve().orElse(null);
    }

    public static boolean isBackpackMenu(Object menu) {
        if (menu == null) {
            return false;
        }
        try {
            Class<?> c = Class.forName("net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase");
            return c.isInstance(menu);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasJEIIndexUpgrade(IBackpackWrapper backpackWrapper) {
        try {
            if (!backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty()) {
                return true;
            }

            byte state = UPGRADE_REFRESH_STATE.getOrDefault(backpackWrapper, (byte) 0);
            if ((state & 1) == 0) {
                UPGRADE_REFRESH_STATE.put(backpackWrapper, (byte) (state | 1));
                backpackWrapper.onContentsNbtUpdated();
                boolean detected = !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
                if (!detected) {
                    UPGRADE_REFRESH_STATE.put(backpackWrapper, (byte) (state | 1 | 2));
                }
                return detected;
            }

            if ((state & 2) == 0) {
                UPGRADE_REFRESH_STATE.put(backpackWrapper, (byte) (state | 2));
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
        }
    }
}
