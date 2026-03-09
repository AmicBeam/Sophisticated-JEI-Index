package com.sbjeiindex;

import com.sbjeiindex.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SBJEIIndex.MOD_ID)
public class SBJEIIndex {
    public static final String MOD_ID = "sophisticated_jei_index";
    private static final ResourceKey<CreativeModeTab> SOPHISTICATED_BACKPACKS_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("sophisticatedbackpacks", "main"));

    public SBJEIIndex() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == SOPHISTICATED_BACKPACKS_TAB) {
            event.accept(ModItems.JEI_INDEX_UPGRADE);
        }
    }
}
