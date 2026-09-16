package com.sbjeiindex.mixin.ae2;

import com.sbjeiindex.util.BackpackExtraction;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(targets = "appeng.menu.me.items.CraftingTermMenu", remap = false)
public class CraftingTermMenuMixin {
    @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true, remap = false)
    private void sbjeiindex_findMissingIngredients(Map<Integer, Ingredient> ingredients, CallbackInfoReturnable<Object> cir) {
        Object result = cir.getReturnValue();
        if (result == null) {
            return;
        }

        Set<Integer> missingSlots = getSet(result, "missingSlots");
        Set<Integer> craftableSlots = getSet(result, "craftableSlots");
        if ((missingSlots == null || missingSlots.isEmpty()) && (craftableSlots == null || craftableSlots.isEmpty())) {
            return;
        }

        Inventory playerInventory;
        try {
            playerInventory = (Inventory) this.getClass().getMethod("getPlayerInventory").invoke(this);
        } catch (Exception e) {
            return;
        }

        Player player = playerInventory.player;
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (handlers.isEmpty()) {
            return;
        }
        List<int[]> reserved = BackpackExtraction.createReservedCounts(handlers);
        Set<Integer> newMissing = missingSlots == null ? new HashSet<>() : new HashSet<>(missingSlots);
        Set<Integer> newCraftable = craftableSlots == null ? new HashSet<>() : new HashSet<>(craftableSlots);

        Set<Integer> toCheck = new HashSet<>();
        toCheck.addAll(newMissing);
        toCheck.addAll(newCraftable);

        for (Integer key : toCheck) {
            Ingredient ingredient = ingredients.get(key);
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }
            if (BackpackExtraction.reserveIngredient(handlers, reserved, ingredient)) {
                newMissing.remove(key);
                newCraftable.remove(key);
            }
        }

        if (newMissing.equals(missingSlots) && newCraftable.equals(craftableSlots)) {
            return;
        }

        try {
            Constructor<?> ctor = result.getClass().getConstructor(Set.class, Set.class);
            cir.setReturnValue(ctor.newInstance(newMissing, newCraftable));
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> getSet(Object record, String accessor) {
        try {
            Object value = record.getClass().getMethod(accessor).invoke(record);
            if (value instanceof Set<?> set) {
                return (Set<Integer>) set;
            }
        } catch (Exception e) {
        }
        return null;
    }
}
