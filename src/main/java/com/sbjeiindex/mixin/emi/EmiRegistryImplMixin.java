package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.emi.VanillaCraftingEmiHandler;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.registry.EmiRegistryImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EmiRegistryImpl.class, remap = false)
public class EmiRegistryImplMixin {
    @Inject(method = "addRecipeHandler", at = @At("TAIL"), remap = false)
    private <T extends AbstractContainerMenu> void sbjeiindex_prioritizeHandler(
        MenuType<T> type, EmiRecipeHandler<T> handler, CallbackInfo ci
    ) {
        if (!(handler instanceof VanillaCraftingEmiHandler)) {
            return;
        }
        List<EmiRecipeHandler<?>> handlers = EmiRecipeFiller.handlers.get(type);
        if (handlers != null && handlers.remove(handler)) {
            handlers.add(0, handler);
        }
    }
}
