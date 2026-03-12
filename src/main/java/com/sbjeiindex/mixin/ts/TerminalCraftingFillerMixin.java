package com.sbjeiindex.mixin.ts;

import com.sbjeiindex.util.BackpackHelper;
import com.sbjeiindex.util.BackpackExtraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(targets = "com.tom.storagemod.util.TerminalCraftingFiller", remap = false)
public class TerminalCraftingFillerMixin {
    @Inject(method = "placeRecipe", at = @At("TAIL"), remap = false)
    private void sbjeiindex_placeRecipe(Recipe<?> recipe, CallbackInfo ci) {
        if (recipe == null) {
            return;
        }

        Object te = getFieldValue(this, "te");
        Player player = (Player) getFieldValue(this, "player");
        if (te == null || player == null) {
            return;
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }

        int rw = getRecipeWidth(recipe, ingredients.size());
        if (rw <= 0) {
            return;
        }

        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }

        Object craftingInvObj;
        try {
            craftingInvObj = te.getClass().getMethod("getCraftingInv").invoke(te);
        } catch (Exception e) {
            return;
        }

        if (!(craftingInvObj instanceof net.minecraft.world.inventory.CraftingContainer craftingInv)) {
            return;
        }

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing == null || ing.isEmpty()) {
                continue;
            }

            int x = i % rw;
            int y = i / rw;
            if (x < 0 || x >= 3 || y < 0 || y >= 3) {
                continue;
            }

            int index = x + y * 3;
            if (!craftingInv.getItem(index).isEmpty()) {
                continue;
            }

            ItemStack extracted = BackpackExtraction.extractIngredient(handlers, ing);
            if (extracted.isEmpty()) {
                continue;
            }

            try {
                te.getClass().getMethod("setCraftSlot", int.class, int.class, ItemStack.class).invoke(te, x, y, extracted);
            } catch (Exception e) {
                craftingInv.setItem(index, extracted);
            }
        }
    }

    private static int getRecipeWidth(Recipe<?> recipe, int ingredientCount) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth();
        }
        int rw = 0;
        while (rw * rw < ingredientCount) {
            rw++;
        }
        return rw;
    }

    private static Object getFieldValue(Object instance, String name) {
        try {
            Field f = instance.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(instance);
        } catch (Exception e) {
            return null;
        }
    }
}
