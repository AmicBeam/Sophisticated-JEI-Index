package com.sbjeiindex.util;

import com.sbjeiindex.config.SBJEIIndexConfig;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
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
import java.util.function.Function;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<IBackpackWrapper, Boolean> UPGRADE_REFRESH_ATTEMPTED = new WeakHashMap<>();
    private static Class<?> STORAGE_MENU_CLASS;
    private static boolean STORAGE_MENU_CLASS_CHECKED;

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
        return collectHandlers(wrappers, IBackpackWrapper::getInventoryHandler);
    }

    public static List<IItemHandlerModifiable> getEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        return collectHandlers(wrappers, IBackpackWrapper::getInventoryHandler);
    }

    @Nullable
    private static IBackpackWrapper getBackpackWrapper(ItemStack stack) {
        try {
            return BackpackWrapper.fromStack(stack);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isBackpackMenu(Object menu) {
        if (menu == null) {
            return false;
        }
        if (!STORAGE_MENU_CLASS_CHECKED) {
            try {
                STORAGE_MENU_CLASS = Class.forName("net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase");
            } catch (Exception e) {
                STORAGE_MENU_CLASS = null;
            } finally {
                STORAGE_MENU_CLASS_CHECKED = true;
            }
        }
        return STORAGE_MENU_CLASS != null && STORAGE_MENU_CLASS.isInstance(menu);
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

    private static <T> List<T> collectHandlers(List<IBackpackWrapper> wrappers, Function<IBackpackWrapper, T> getter) {
        List<T> handlers = new ArrayList<>(wrappers.size());
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IBackpackWrapper wrapper : wrappers) {
            T h = getter.apply(wrapper);
            if (h != null && seen.add(h)) {
                handlers.add(h);
            }
        }
        return handlers;
    }
}
