package com.sbjeiindex.jei;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * A virtual backpack slot used by JEI recipe transfer.
 *
 * <p>Forge's {@link SlotItemHandler} reports empty slots as not modifiable because
 * both {@code mayPickup} and {@code mayPlace(ItemStack.EMPTY)} return false. JEI
 * 15.21 validates every transfer inventory slot with {@code allowModification},
 * including empty slots, so the default behavior makes an otherwise writable
 * backpack fail recipe-transfer preflight.</p>
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
