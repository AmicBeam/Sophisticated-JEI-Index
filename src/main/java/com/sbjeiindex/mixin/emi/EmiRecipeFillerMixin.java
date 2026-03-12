package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.emi.EmiBackpackSlots;
import com.sbjeiindex.config.SBJEIIndexConfig;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
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
        List<Slot> sources = handler.getInputSources(menu);
        if (!EmiClient.onServer || !SBJEIIndexConfig.enableEmi.get()) {
            return sources;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return sources;
        }

        List<Slot> extra = EmiBackpackSlots.create(player);
        if (extra.isEmpty()) {
            return sources;
        }

        List<Slot> combined = new ArrayList<>(sources.size() + extra.size());
        combined.addAll(sources);
        combined.addAll(extra);
        return combined;
    }
}
