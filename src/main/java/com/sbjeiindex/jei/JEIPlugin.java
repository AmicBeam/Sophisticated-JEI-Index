package com.sbjeiindex.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import com.sbjeiindex.SBJEIIndex;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(SBJEIIndex.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    }
}
