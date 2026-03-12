package com.sbjeiindex.mixin;

import com.sbjeiindex.jei.JeiSlotResolver;
import com.sbjeiindex.jei.JeiTransferConstants;
import com.sbjeiindex.jei.OffsetItemHandlerModifiable;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.network.packets.PacketRecipeTransfer;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(value = BasicRecipeTransferHandler.class, remap = false)
public class BasicRecipeTransferHandlerMixin {
    @Shadow(remap = false)
    private IConnectionToServer serverConnection;

    @Shadow(remap = false)
    private IStackHelper stackHelper;

    @Shadow(remap = false)
    private IRecipeTransferHandlerHelper handlerHelper;

    @Shadow(remap = false)
    private IRecipeTransferInfo transferInfo;

    @Inject(method = "transferRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbjeiindex_transferRecipe(
        AbstractContainerMenu container,
        Object recipe,
        IRecipeSlotsView recipeSlotsView,
        Player player,
        boolean maxTransfer,
        boolean doTransfer,
        CallbackInfoReturnable<IRecipeTransferError> cir
    ) {
        List<IItemHandlerModifiable> backpackHandlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        if (backpackHandlers.isEmpty()) {
            return;
        }

        if (!serverConnection.isJeiOnServer()) {
            Component tooltipMessage = Component.translatable("jei.tooltip.error.recipe.transfer.no.server");
            cir.setReturnValue(handlerHelper.createUserErrorWithTooltip(tooltipMessage));
            return;
        }

        if (!transferInfo.canHandle(container, recipe)) {
            IRecipeTransferError handlingError = transferInfo.getHandlingError(container, recipe);
            if (handlingError != null) {
                cir.setReturnValue(handlingError);
                return;
            }
            cir.setReturnValue(handlerHelper.createInternalError());
            return;
        }

        List<Slot> craftingSlots = List.copyOf(transferInfo.getRecipeSlots(container, recipe));
        List<Slot> inventorySlotsRaw = List.copyOf(transferInfo.getInventorySlots(container, recipe));
        if (!BasicRecipeTransferHandler.validateTransferInfo(transferInfo, container, craftingSlots, inventorySlotsRaw, player)) {
            cir.setReturnValue(handlerHelper.createInternalError());
            return;
        }

        List<IRecipeSlotView> inputItemSlotViews = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);
        if (!BasicRecipeTransferHandler.validateRecipeView(transferInfo, container, craftingSlots, inputItemSlotViews)) {
            cir.setReturnValue(handlerHelper.createInternalError());
            return;
        }

        Set<Integer> craftingSlotIndexes = craftingSlots.stream().map(s -> s.index).collect(Collectors.toSet());
        List<Slot> inventorySlots = inventorySlotsRaw.stream()
            .filter(s -> !craftingSlotIndexes.contains(s.index))
            .toList();

        int backpackSlotCount = 0;
        for (IItemHandlerModifiable handler : backpackHandlers) {
            backpackSlotCount += handler.getSlots();
        }

        List<Slot> extendedInventorySlots = new ArrayList<>(inventorySlots.size() + backpackSlotCount);
        extendedInventorySlots.addAll(inventorySlots);

        Map<Integer, Slot> extraSlots = new HashMap<>();
        Map<Slot, net.minecraft.world.item.ItemStack> availableItemStacks = new HashMap<>();

        int filledCraftSlotCount = 0;
        for (Slot slot : craftingSlots) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                if (!slot.mayPickup(player)) {
                    cir.setReturnValue(handlerHelper.createInternalError());
                    return;
                }
                if (slot.mayPlace(stack)) {
                    filledCraftSlotCount++;
                    if (slot.allowModification(player)) {
                        availableItemStacks.put(slot, stack.copy());
                    }
                }
            }
        }

        int emptySlots = 0;
        for (Slot slot : inventorySlots) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                if (!slot.mayPickup(player)) {
                    cir.setReturnValue(handlerHelper.createInternalError());
                    return;
                }
                if (slot.allowModification(player)) {
                    availableItemStacks.put(slot, stack.copy());
                }
            } else {
                emptySlots++;
            }
        }

        for (int backpackIndex = 0; backpackIndex < backpackHandlers.size(); backpackIndex++) {
            IItemHandlerModifiable backpackHandler = backpackHandlers.get(backpackIndex);
            int baseOffset = JeiTransferConstants.BACKPACK_SLOT_ID_OFFSET + backpackIndex * JeiTransferConstants.BACKPACK_SLOT_ID_STRIDE;
            OffsetItemHandlerModifiable offsetHandler = new OffsetItemHandlerModifiable(backpackHandler, baseOffset);

            for (int i = 0; i < backpackHandler.getSlots(); i++) {
                int slotId = baseOffset + i;
                Slot slot = new SlotItemHandler(offsetHandler, slotId, 0, 0);
                slot.index = slotId;
                extendedInventorySlots.add(slot);
                extraSlots.put(slotId, slot);

                net.minecraft.world.item.ItemStack stack = offsetHandler.getStackInSlot(slotId);
                if (!stack.isEmpty()) {
                    availableItemStacks.put(slot, stack.copy());
                } else {
                    emptySlots++;
                }
            }
        }

        int inputCount = inputItemSlotViews.size();
        if (filledCraftSlotCount - inputCount > emptySlots) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full");
            cir.setReturnValue(handlerHelper.createUserErrorWithTooltip(message));
            return;
        }

        RecipeTransferOperationsResult transferOperations = RecipeTransferUtil.getRecipeTransferOperations(
            stackHelper,
            availableItemStacks,
            inputItemSlotViews,
            craftingSlots
        );

        if (!transferOperations.missingItems.isEmpty()) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
            cir.setReturnValue(handlerHelper.createUserErrorForMissingSlots(message, transferOperations.missingItems));
            return;
        }

        JeiSlotResolver.set(extraSlots);
        try {
            if (!RecipeTransferUtil.validateSlots(player, transferOperations.results, craftingSlots, extendedInventorySlots)) {
                cir.setReturnValue(handlerHelper.createInternalError());
                return;
            }

            if (doTransfer) {
                boolean requireCompleteSets = transferInfo.requireCompleteSets(container, recipe);
                PacketRecipeTransfer packet = new PacketRecipeTransfer(
                    transferOperations.results,
                    craftingSlots,
                    extendedInventorySlots,
                    maxTransfer,
                    requireCompleteSets
                );
                serverConnection.sendPacketToServer(packet);
            }
        } finally {
            JeiSlotResolver.clear();
        }

        cir.setReturnValue(null);
    }
}
