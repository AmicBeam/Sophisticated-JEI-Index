package com.sbjeiindex.util;

import com.sbjeiindex.config.SBJEIIndexConfig;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
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
            if (isEligibleBackpack(wrapper)) {
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

    public static List<IItemHandlerModifiable> getEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        List<IItemHandlerModifiable> handlers = new ArrayList<>(wrappers.size());
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IBackpackWrapper wrapper : wrappers) {
            IItemHandlerModifiable handler = asLegacyItemHandler(wrapper.getInventoryForUpgradeProcessing());
            if (handler != null && seen.add(handler)) {
                handlers.add(handler);
            }
        }
        return handlers;
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

    private static boolean isEligibleBackpack(IBackpackWrapper backpackWrapper) {
        return SBJEIIndexConfig.enableTransferWithoutUpgrade.get() || hasJEIIndexUpgrade(backpackWrapper);
    }

    private static boolean hasJEIIndexUpgrade(IBackpackWrapper backpackWrapper) {
        try {
            if (!backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty()) {
                return true;
            }

            if (UPGRADE_REFRESH_ATTEMPTED.putIfAbsent(backpackWrapper, Boolean.TRUE) == null) {
                refreshUpgradeHandlers(backpackWrapper);
                return !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
        }
    }

    private static void refreshUpgradeHandlers(IBackpackWrapper backpackWrapper) {
        invokeNoArg(backpackWrapper, "refreshInventoryForUpgradeProcessing");
        invokeNoArg(backpackWrapper, "onContentsUpdated");
    }

    private static void invokeNoArg(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
        }
    }

    @Nullable
    private static IItemHandlerModifiable asLegacyItemHandler(ITrackedContentsItemResourceHandler handler) {
        if (handler instanceof IItemHandlerModifiable itemHandler) {
            return itemHandler;
        }
        if (handler instanceof InventoryHandler inventoryHandler) {
            return new InventoryHandlerAdapter(inventoryHandler);
        }
        return null;
    }

    private record InventoryHandlerAdapter(InventoryHandler inventoryHandler) implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return inventoryHandler.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventoryHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = inventoryHandler.getStackInSlot(slot);
            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            int extractedCount = Math.min(amount, stack.getCount());
            ItemStack extracted = stack.copyWithCount(extractedCount);
            if (!simulate) {
                ItemStack remaining = stack.copy();
                remaining.shrink(extractedCount);
                inventoryHandler.setStackInSlot(slot, remaining);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventoryHandler.getInternalSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventoryHandler.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            inventoryHandler.setStackInSlot(slot, stack);
        }
    }
}
