package com.sbjeiindex.mixin.rs;

import com.sbjeiindex.util.BackpackExtraction;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(targets = "com.refinedmods.refinedstorage.integration.jei.IngredientTracker", remap = false)
public class IngredientTrackerMixin {
    @Inject(method = "updateAvailability", at = @At("RETURN"), remap = false)
    private void sbjeiindex_updateAvailability(@Coerce Object ingredientList, @Coerce Object gridContainer, Player player, CallbackInfo ci) {
        if (ingredientList == null || player == null) {
            return;
        }

        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }
        List<int[]> reserved = BackpackExtraction.createReservedCounts(handlers);

        List<?> ingredients = getIngredients(ingredientList);
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        for (Object ingredient : ingredients) {
            int missing = invokeInt(ingredient, "getMissingAmount");
            if (missing <= 0) {
                continue;
            }

            Object slotViewObj = invoke(ingredient, "getSlotView");
            if (!(slotViewObj instanceof IRecipeSlotView slotView)) {
                continue;
            }

            ItemStack[] templates = slotView.getItemStacks().toArray(ItemStack[]::new);
            if (templates.length == 0) {
                continue;
            }

            while (missing > 0) {
                if (!BackpackExtraction.reserveFirstTemplateMatch(handlers, reserved, templates)) {
                    break;
                }
                invokeVoid(ingredient, "fulfill", int.class, 1);
                missing--;
            }
        }
    }

    private static List<?> getIngredients(Object ingredientList) {
        try {
            Field f = ingredientList.getClass().getDeclaredField("ingredients");
            f.setAccessible(true);
            Object v = f.get(ingredientList);
            if (v instanceof List<?> list) {
                return list;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static Object invoke(Object target, String name, Class<?>... params) {
        try {
            Method m = target.getClass().getMethod(name, params);
            return m.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static int invokeInt(Object target, String name, Class<?>... params) {
        Object v = invoke(target, name, params);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static void invokeVoid(Object target, String name, Class<?> param, Object arg) {
        try {
            Method m = target.getClass().getMethod(name, param);
            m.invoke(target, arg);
        } catch (Exception e) {
        }
    }
}
