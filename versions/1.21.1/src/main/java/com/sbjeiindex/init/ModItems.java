package com.sbjeiindex.init;

import com.sbjeiindex.SBJEIIndex;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SBJEIIndex.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.item.Item, JEIIndexUpgradeItem> JEI_INDEX_UPGRADE =
            ITEMS.register("jei_index_upgrade", JEIIndexUpgradeItem::new);
}
