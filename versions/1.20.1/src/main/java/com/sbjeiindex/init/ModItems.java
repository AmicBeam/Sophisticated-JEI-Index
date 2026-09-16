package com.sbjeiindex.init;

import com.sbjeiindex.SBJEIIndex;
import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SBJEIIndex.MOD_ID);

    public static final RegistryObject<JEIIndexUpgradeItem> JEI_INDEX_UPGRADE =
            ITEMS.register("jei_index_upgrade", JEIIndexUpgradeItem::new);
}
