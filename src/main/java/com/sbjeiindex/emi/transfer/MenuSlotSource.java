package com.sbjeiindex.emi.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MenuSlotSource implements EmiInputSource {
    private final Slot slot;

    public MenuSlotSource(Slot slot) {
        this.slot = slot;
    }

    @Override
    public ItemStack getItem() {
        return slot.getItem();
    }

    @Override
    public boolean mayPickup(Player player) {
        return slot.mayPickup(player);
    }

    @Override
    public boolean isCrafting(List<Slot> crafting) {
        return crafting.contains(slot);
    }

    @Override
    public int extract(Player player, int amount, ItemStack current) {
        if (amount <= 0 || current.isEmpty()) {
            return 0;
        }
        ItemStack snapshot = current.copy();
        if (current.getCount() <= amount) {
            int taken = current.getCount();
            slot.set(ItemStack.EMPTY);
            slot.onTake(player, snapshot);
            return taken;
        }
        current.shrink(amount);
        slot.onTake(player, snapshot);
        return amount;
    }
}
