package com.sbjeiindex;

import com.sbjeiindex.init.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SBJEIIndex.MOD_ID)
public class SBJEIIndex {
    public static final String MOD_ID = "sophisticated_jei_index";

    public SBJEIIndex() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 尝试将物品添加到Sophisticated Backpacks标签
        try {
            Class<?> creativeModeTabsClass = Class.forName("net.puffish.sophisticatedbackpacks.init.ModCreativeModeTabs");
            Object backpacksTab = creativeModeTabsClass.getField("BACKPACKS").get(null);
            if (event.getTab() == backpacksTab) {
                event.accept(ModItems.JEI_INDEX_UPGRADE);
            }
        } catch (Exception e) {
            // 如果Sophisticated Backpacks标签不存在，回退到INGREDIENTS标签
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                event.accept(ModItems.JEI_INDEX_UPGRADE);
            }
        }
    }
}
