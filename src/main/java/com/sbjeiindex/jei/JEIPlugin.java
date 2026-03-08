package com.sbjeiindex.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import com.sbjeiindex.SBJEIIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation PLUGIN_UID = new ResourceLocation(SBJEIIndex.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        LOGGER.info("Registering JEI recipe transfer handlers");
        
        mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper transferHelper = registration.getTransferHelper();
        mezz.jei.api.helpers.IStackHelper stackHelper = registration.getJeiHelpers().getStackHelper();
        
        // 注册合成台配方转移处理器
        RecipeType<net.minecraft.world.item.crafting.CraftingRecipe> craftingType = mezz.jei.api.constants.RecipeTypes.CRAFTING;
        registration.addRecipeTransferHandler(new JEICraftingTransferHandler(transferHelper, stackHelper), craftingType);
        
        // 注册切石机配方转移处理器
        RecipeType<net.minecraft.world.item.crafting.StonecutterRecipe> stonecuttingType = mezz.jei.api.constants.RecipeTypes.STONECUTTING;
        registration.addRecipeTransferHandler(new JEIStonecuttingTransferHandler(transferHelper, stackHelper), stonecuttingType);
        
        LOGGER.info("JEI recipe transfer handlers registered successfully");
    }
}
