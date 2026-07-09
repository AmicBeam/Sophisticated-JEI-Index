package com.sbjeiindex.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import com.sbjeiindex.SBJEIIndex;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final Identifier PLUGIN_UID = Identifier.fromNamespaceAndPath(SBJEIIndex.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    }
}
