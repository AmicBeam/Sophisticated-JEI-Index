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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<IBackpackWrapper, Boolean> UPGRADE_REFRESH_ATTEMPTED = new WeakHashMap<>();

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

    @Nullable
    public static IItemHandlerModifiable getBackpackItemHandler(ItemStack stack) {
        IBackpackWrapper wrapper = getBackpackWrapper(stack);
        if (wrapper == null) {
            return null;
        }
        return wrapper.getInventoryHandler();
    }

    @Nullable
    public static IItemHandlerModifiable getVisibleBackpackItemHandler(Object menu) {
        if (menu == null) {
            return null;
        }
        try {
            Method m = menu.getClass().getMethod("getVisibleStorageItem");
            Object v = m.invoke(menu);
            if (v instanceof ItemStack stack && !stack.isEmpty()) {
                return getBackpackItemHandler(stack);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static boolean hasJEIIndexUpgrade(IBackpackWrapper backpackWrapper) {
        try {
            if (!backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty()) {
                return true;
            }

            if (UPGRADE_REFRESH_ATTEMPTED.putIfAbsent(backpackWrapper, Boolean.TRUE) == null) {
                backpackWrapper.onContentsNbtUpdated();
                return !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
            }

            return false;
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
        }
    }
}
