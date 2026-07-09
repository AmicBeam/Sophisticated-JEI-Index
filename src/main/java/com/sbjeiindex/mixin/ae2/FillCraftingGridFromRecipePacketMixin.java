package com.sbjeiindex.mixin.ae2;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackExtraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "appeng.core.network.serverbound.FillCraftingGridFromRecipePacket", remap = false)
public class FillCraftingGridFromRecipePacketMixin {
    @Inject(method = "takeIngredientFromPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void sbjeiindex_takeIngredientFromPlayer(@Coerce Object cct, ServerPlayer player, Ingredient ingredient, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack current = cir.getReturnValue();
        if (current != null && !current.isEmpty()) {
            return;
        }

        List<IItemHandlerModifiable> handlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }

        ItemStack extracted = BackpackExtraction.extractIngredient(handlers, ingredient);
        if (!extracted.isEmpty()) {
            cir.setReturnValue(extracted);
        }
    }
}
