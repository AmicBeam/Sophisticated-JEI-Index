package com.sbjeiindex.emi.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface EmiInputSource {
    ItemStack getItem();

    boolean mayPickup(Player player);

    boolean isCrafting(List<Slot> crafting);

    int extract(Player player, int amount, ItemStack current);
}
