package com.sbjeiindex.util;

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
import java.util.List;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    public static List<IBackpackWrapper> getEquippedBackpacksWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> results = new ArrayList<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
            IBackpackWrapper wrapper = getBackpackWrapperFromStack(backpack);
            if (wrapper == null) {
                return false;
            }
            if (hasJEIIndexUpgrade(wrapper)) {
                results.add(wrapper);
            }
            return false;
        });
        return results;
    }

    public static List<InventoryHandler> getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        List<InventoryHandler> handlers = new ArrayList<>(wrappers.size());
        for (IBackpackWrapper wrapper : wrappers) {
            InventoryHandler h = wrapper.getInventoryHandler();
            if (h != null) {
                handlers.add(h);
            }
        }
        return handlers;
    }

    public static List<IItemHandlerModifiable> getEquippedBackpackItemHandlersWithJEIIndexUpgrade(Player player) {
        List<IBackpackWrapper> wrappers = getEquippedBackpacksWithJEIIndexUpgrade(player);
        List<IItemHandlerModifiable> handlers = new ArrayList<>(wrappers.size());
        for (IBackpackWrapper wrapper : wrappers) {
            IItemHandlerModifiable h = wrapper.getInventoryHandler();
            if (h != null) {
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
    private static IBackpackWrapper getBackpackWrapperFromStack(ItemStack stack) {
        LazyOptional<IBackpackWrapper> cap = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance());
        return cap.resolve().orElse(null);
    }

    private static boolean hasJEIIndexUpgrade(IBackpackWrapper backpackWrapper) {
        try {
            backpackWrapper.onContentsNbtUpdated();
            return !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
        }
    }
}
