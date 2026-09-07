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
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider.BackpackInventorySlotConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Method RUN_ON_BACKPACKS = findRunOnBackpacksMethod();
    private static Class<?> STORAGE_MENU_CLASS;
    private static boolean STORAGE_MENU_CLASS_CHECKED;

    public static List<IBackpackWrapper> getEquippedBackpacksWithJEIIndexUpgrade(Player player) {
        return getIndexedEquippedBackpacksWithJEIIndexUpgrade(player).stream()
            .map(IndexedBackpack::wrapper)
            .toList();
    }

    private static List<IndexedBackpack> getIndexedEquippedBackpacksWithJEIIndexUpgrade(Player player) {
        int maxScanned = SBJEIIndexConfig.maxEnabledBackpacksScanned.get();
        List<IndexedBackpack> results = new ArrayList<>();
        Set<IBackpackWrapper> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] backpackIndex = {0};
        runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
            int index = backpackIndex[0]++;
            IBackpackWrapper wrapper = getBackpackWrapper(backpack);
            if (wrapper == null) {
                return false;
            }
            if (player.level().isClientSide()) {
                BackpackClientContentsSync.registerAndRequest(wrapper);
            }
            if (isEligibleBackpack(wrapper)) {
                if (seen.add(wrapper)) {
                    results.add(new IndexedBackpack(index, wrapper));
                    if (maxScanned > 0 && results.size() >= maxScanned) {
                        return true;
                    }
                }
            }
            return false;
        });
        return results;
    }

    // Sophisticated Backpacks 3.26 changed this method's return type from void to boolean.
    // Reflection keeps this release compatible with both binary descriptors.
    private static void runOnBackpacks(Player player, BackpackInventorySlotConsumer consumer) {
        if (RUN_ON_BACKPACKS == null) {
            return;
        }
        try {
            RUN_ON_BACKPACKS.invoke(PlayerInventoryProvider.get(), player, consumer);
        } catch (IllegalAccessException e) {
            LOGGER.error("Unable to access Sophisticated Backpacks inventory provider", e);
        } catch (InvocationTargetException e) {
            LOGGER.error("Error while scanning equipped Sophisticated Backpacks", e.getCause());
        }
    }

    @Nullable
    private static Method findRunOnBackpacksMethod() {
        try {
            return PlayerInventoryProvider.class.getMethod(
                "runOnBackpacks", Player.class, BackpackInventorySlotConsumer.class
            );
        } catch (NoSuchMethodException e) {
            LOGGER.error("Sophisticated Backpacks does not expose a compatible backpack inventory scanner", e);
            return null;
        }
    }

    public static List<IItemHandlerModifiable> getEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        return getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(player).stream()
            .map(IndexedBackpackHandler::handler)
            .toList();
    }

    public static List<InventoryHandler> getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(Player player) {
        List<InventoryHandler> handlers = new ArrayList<>();
        Set<InventoryHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IndexedBackpack backpack : getIndexedEquippedBackpacksWithJEIIndexUpgrade(player)) {
            if (backpack.wrapper().getInventoryForUpgradeProcessing() instanceof InventoryHandler handler && seen.add(handler)) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    public static List<IndexedBackpackHandler> getIndexedEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        List<IndexedBackpack> backpacks = getIndexedEquippedBackpacksWithJEIIndexUpgrade(player);
        List<IndexedBackpackHandler> handlers = new ArrayList<>(backpacks.size());
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IndexedBackpack backpack : backpacks) {
            IItemHandlerModifiable handler = asLegacyItemHandler(backpack.wrapper().getInventoryForUpgradeProcessing());
            if (handler != null && seen.add(handler)) {
                handlers.add(new IndexedBackpackHandler(backpack.index(), handler));
            }
        }
        return handlers;
    }

    private record IndexedBackpack(int index, IBackpackWrapper wrapper) {}

    public record IndexedBackpackHandler(int index, IItemHandlerModifiable handler) {}

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
            return !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
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
