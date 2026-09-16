package com.sbjeiindex.emi.transfer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;

public class BackpackSlotSource implements EmiInputSource {
    private final IItemHandlerModifiable handler;
    private final int slot;

    public BackpackSlotSource(IItemHandlerModifiable handler, int slot) {
        this.handler = handler;
        this.slot = slot;
    }

    @Override
    public ItemStack getItem() {
        return handler.getStackInSlot(slot);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.extractItem(slot, 1, true).isEmpty();
    }

    @Override
    public boolean isCrafting(List<Slot> crafting) {
        return false;
    }

    @Override
    public int extract(Player player, int amount, ItemStack current) {
        if (amount <= 0 || current.isEmpty()) {
            return 0;
        }
        int available = current.getCount();
        int toExtract = Math.min(available, amount);
        if (toExtract <= 0) {
            return 0;
        }
        ItemStack extracted = handler.extractItem(slot, toExtract, false);
        return extracted.getCount();
    }
}
