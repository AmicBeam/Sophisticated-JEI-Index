package com.sbjeiindex.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class OffsetItemHandlerModifiable implements IItemHandlerModifiable {
    private final IItemHandlerModifiable delegate;
    private final int offset;

    public OffsetItemHandlerModifiable(IItemHandlerModifiable delegate, int offset) {
        this.delegate = delegate;
        this.offset = offset;
    }

    private int toDelegateIndex(int slot) {
        return slot - offset;
    }

    @Override
    public int getSlots() {
        return offset + delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return ItemStack.EMPTY;
        }
        return delegate.getStackInSlot(i);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return stack;
        }
        return delegate.insertItem(i, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return ItemStack.EMPTY;
        }
        return delegate.extractItem(i, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return 0;
        }
        return delegate.getSlotLimit(i);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return false;
        }
        return delegate.isItemValid(i, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int i = toDelegateIndex(slot);
        if (i < 0 || i >= delegate.getSlots()) {
            return;
        }
        delegate.setStackInSlot(i, stack);
    }
}
