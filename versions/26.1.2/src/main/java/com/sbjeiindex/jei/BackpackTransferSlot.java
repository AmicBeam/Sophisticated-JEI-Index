package com.sbjeiindex.jei;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A virtual backpack slot used by JEI recipe transfer.
 *
 * <p>SlotItemHandler reports empty slots as not modifiable, while recent JEI
 * versions validate every transfer inventory slot, including empty slots.</p>
 */
public class BackpackTransferSlot extends SlotItemHandler {
    public BackpackTransferSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean allowModification(Player player) {
        return getItem().isEmpty() || super.allowModification(player);
    }
}
