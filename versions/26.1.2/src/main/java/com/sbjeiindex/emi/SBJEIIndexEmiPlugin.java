package com.sbjeiindex.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraft.world.inventory.MenuType;

@EmiEntrypoint
public class SBJEIIndexEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(MenuType.CRAFTING, new VanillaCraftingEmiHandler());
    }
}
