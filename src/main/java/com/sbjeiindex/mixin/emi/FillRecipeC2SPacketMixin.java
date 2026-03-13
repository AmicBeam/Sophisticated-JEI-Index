package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.emi.EmiTransferConstants;
import com.sbjeiindex.emi.transfer.BackpackSlotSource;
import com.sbjeiindex.emi.transfer.EmiFillHelper;
import com.sbjeiindex.emi.transfer.EmiInputSource;
import com.sbjeiindex.emi.transfer.MenuSlotSource;
import com.sbjeiindex.util.BackpackHelper;
import dev.emi.emi.network.FillRecipeC2SPacket;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = FillRecipeC2SPacket.class, remap = false)
public class FillRecipeC2SPacketMixin {
    @Shadow
    @Final
    private int syncId;

    @Shadow
    @Final
    private int action;

    @Shadow
    @Final
    private List<Integer> slots;

    @Shadow
    @Final
    private List<Integer> crafting;

    @Shadow
    @Final
    private int output;

    @Shadow
    @Final
    private List<ItemStack> stacks;

    @Overwrite
    public void apply(Player player) {
        if (slots == null || crafting == null) {
            EmiLog.warn("Client requested fill but passed input and crafting slot information was invalid, aborting");
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != syncId) {
            EmiLog.warn("Client requested fill but screen handler has changed, aborting");
            return;
        }

        List<IItemHandlerModifiable> backpackHandlers = null;
        List<EmiInputSource> inputSources = new ArrayList<>();
        for (int slotId : slots) {
            if (slotId >= 0 && slotId < menu.slots.size()) {
                inputSources.add(new MenuSlotSource(menu.slots.get(slotId)));
            } else if (slotId >= EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET) {
                if (backpackHandlers == null) {
                    backpackHandlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
                }

                int relative = slotId - EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET;
                int backpackIndex = relative / EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
                int backpackSlot = relative % EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
                if (backpackIndex < 0 || backpackIndex >= backpackHandlers.size()) {
                    EmiLog.warn("Client requested fill but passed input slots don't exist, aborting");
                    return;
                }

                IItemHandlerModifiable handler = backpackHandlers.get(backpackIndex);
                if (backpackSlot < 0 || backpackSlot >= handler.getSlots()) {
                    EmiLog.warn("Client requested fill but passed input slots don't exist, aborting");
                    return;
                }

                inputSources.add(new BackpackSlotSource(handler, backpackSlot));
            } else {
                EmiLog.warn("Client requested fill but passed input slots don't exist, aborting");
                return;
            }
        }

        List<Slot> craftingSlots = new ArrayList<>();
        for (int slotId : crafting) {
            if (slotId >= 0 && slotId < menu.slots.size()) {
                craftingSlots.add(menu.slots.get(slotId));
            } else {
                craftingSlots.add(null);
            }
        }

        Slot outputSlot = null;
        if (output != -1 && output >= 0 && output < menu.slots.size()) {
            outputSlot = menu.slots.get(output);
        }

        if (craftingSlots.size() < stacks.size()) {
            return;
        }

        List<ItemStack> cleared = new ArrayList<>();
        try {
            for (Slot slot : craftingSlots) {
                if (slot == null) {
                    continue;
                }
                if (!slot.mayPickup(player)) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                cleared.add(stack.copy());
                slot.set(ItemStack.EMPTY);
                slot.onTake(player, stack);
            }

            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.isEmpty()) {
                    continue;
                }

                int grabbed = EmiFillHelper.grabMatching(player, inputSources, cleared, craftingSlots, stack);
                if (grabbed != stack.getCount()) {
                    if (grabbed > 0) {
                        stack.shrink(grabbed);
                        player.getInventory().placeItemBackInInventory(stack);
                    }
                    return;
                }

                Slot dest = craftingSlots.get(i);
                if (dest != null
                    && dest.mayPlace(stack)
                    && stack.getCount() <= dest.getMaxStackSize()
                    && stack.getCount() <= stack.getMaxStackSize()) {
                    dest.set(stack);
                } else {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }

            if (outputSlot != null) {
                if (action == 1) {
                    menu.clicked(outputSlot.index, 0, ClickType.PICKUP, player);
                } else if (action == 2) {
                    menu.clicked(outputSlot.index, 0, ClickType.QUICK_MOVE, player);
                }
            }
        } finally {
            for (ItemStack s : cleared) {
                player.getInventory().placeItemBackInInventory(s);
            }
        }
    }

}
