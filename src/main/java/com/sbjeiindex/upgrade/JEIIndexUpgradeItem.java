package com.sbjeiindex.upgrade;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;

public class JEIIndexUpgradeItem extends UpgradeItemBase<JEIIndexUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    private static final Logger LOGGER = LogManager.getLogger();

    public JEIIndexUpgradeItem() {
        super(new IUpgradeCountLimitConfig() {
            @Override
            public int getMaxUpgradesInGroupPerStorage(String storageId, UpgradeGroup group) {
                return 1;
            }

            @Override
            public int getMaxUpgradesPerStorage(String storageId, ResourceLocation upgradeId) {
                return 1;
            }
        });
        LOGGER.info("JEI Index Upgrade item created");
    }

    @Override
    public UpgradeType<Wrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of();
    }

    public static class Wrapper extends UpgradeWrapperBase<Wrapper, JEIIndexUpgradeItem> {
        public Wrapper(IStorageWrapper backpackWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
            super(backpackWrapper, upgrade, upgradeSaveHandler);
            LOGGER.info("JEI Index Upgrade wrapper created for backpack: {}", backpackWrapper);
        }

        @Override
        public boolean hideSettingsTab() {
            return true;
        }
    }
}
