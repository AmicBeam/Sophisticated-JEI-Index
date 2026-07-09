package com.sbjeiindex.init;

import com.sbjeiindex.SBJEIIndex;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SBJEIIndex.MOD_ID);

    public static final DeferredHolder<Item, JEIIndexUpgradeItem> JEI_INDEX_UPGRADE =
            ITEMS.register("jei_index_upgrade", () -> new JEIIndexUpgradeItem(itemProperties("jei_index_upgrade")));

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SBJEIIndex.MOD_ID, name)));
    }
}
