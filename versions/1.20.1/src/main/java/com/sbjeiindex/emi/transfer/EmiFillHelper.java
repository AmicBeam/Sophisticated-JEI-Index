package com.sbjeiindex.emi.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class EmiFillHelper {
    private EmiFillHelper() {
    }

    public static int grabMatching(Player player, List<EmiInputSource> sources, List<ItemStack> items, List<Slot> crafting, ItemStack stack) {
        int total = stack.getCount();
        int grabbed = 0;
        int i = 0;
        while (i < items.size()) {
            if (grabbed >= total) {
                return grabbed;
            }
            ItemStack item = items.get(i);
            if (ItemStack.isSameItemSameTags(stack, item)) {
                int remaining = total - grabbed;
                if (item.getCount() <= remaining) {
                    grabbed += item.getCount();
                    items.remove(i);
                    i--;
                } else {
                    grabbed = total;
                    item.shrink(remaining);
                }
            }
            i++;
        }

        for (EmiInputSource source : sources) {
            if (grabbed >= total) {
                return grabbed;
            }
            if (!source.mayPickup(player)) {
                continue;
            }
            if (source.isCrafting(crafting)) {
                continue;
            }
            ItemStack item = source.getItem();
            if (!ItemStack.isSameItemSameTags(stack, item)) {
                continue;
            }
            int remaining = total - grabbed;
            int extracted = source.extract(player, remaining, item);
            grabbed += extracted;
        }
        return grabbed;
    }
}
