package com.sbjeiindex.util;

import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static IBackpackWrapper getEquippedBackpackWithJEIIndexUpgrade(Player player) {
        AtomicReference<IBackpackWrapper> result = new AtomicReference<>();
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryHandlerName, identifier, slot) -> {
            IBackpackWrapper wrapper = getBackpackWrapperFromStack(backpack);
            if (wrapper == null) {
                return false;
            }
            if (hasJEIIndexUpgrade(wrapper)) {
                result.set(wrapper);
                return true;
            }
            return false;
        });
        return result.get();
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
