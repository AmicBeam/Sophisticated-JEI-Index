package com.sbjeiindex.jei;

import com.sbjeiindex.SBJEIIndex;
import com.sbjeiindex.config.SBJEIIndexConfig;
import com.sbjeiindex.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (SBJEIIndexConfig.enableTransferWithoutUpgrade.get()) {
            jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModItems.JEI_INDEX_UPGRADE.get()))
            );
        }
    }
}
