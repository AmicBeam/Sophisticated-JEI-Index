package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.emi.EmiInputSourcesHelper;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
@Mixin(value = EmiRecipeFiller.class, remap = false)
public class EmiRecipeFillerMixin {
    @Redirect(
        method = "getStacks",
        at = @At(
            value = "INVOKE",
            target = "Ldev/emi/emi/api/recipe/handler/StandardRecipeHandler;getInputSources(Lnet/minecraft/world/inventory/AbstractContainerMenu;)Ljava/util/List;"
        )
    )
    private static <T extends AbstractContainerMenu> List<Slot> sbjeiindex_extendInputSources(StandardRecipeHandler<T> handler, T menu) {
        return EmiInputSourcesHelper.extend(handler, menu);
    }
}
