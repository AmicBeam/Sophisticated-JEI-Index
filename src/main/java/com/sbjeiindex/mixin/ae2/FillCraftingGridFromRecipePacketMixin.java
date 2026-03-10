package com.sbjeiindex.mixin.ae2;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackExtraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.core.sync.packets.FillCraftingGridFromRecipePacket", remap = false)
public class FillCraftingGridFromRecipePacketMixin {
    @Inject(method = "takeIngredientFromPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void sbjeiindex_takeIngredientFromPlayer(@Coerce Object cct, ServerPlayer player, Ingredient ingredient, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack current = cir.getReturnValue();
        if (current != null && !current.isEmpty()) {
            return;
        }

        IBackpackWrapper wrapper = BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
        if (wrapper == null) {
            return;
        }

        ItemStack extracted = BackpackExtraction.extractIngredient(wrapper.getInventoryHandler(), ingredient);
        if (!extracted.isEmpty()) {
            cir.setReturnValue(extracted);
        }
    }
}
