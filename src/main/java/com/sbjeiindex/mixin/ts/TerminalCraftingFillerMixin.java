package com.sbjeiindex.mixin.ts;

import com.sbjeiindex.util.BackpackExtraction;
import com.sbjeiindex.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "com.tom.storagemod.menu.TerminalCraftingFiller", remap = false)
public class TerminalCraftingFillerMixin {
    @Shadow
    private Player player;

    @Shadow
    public void accountStack(ItemStack st) {
    }

    private static final int BACKPACK_SLOT_ID = -2;
    private static final ThreadLocal<ItemStack> BACKPACK_TEMPLATE = new ThreadLocal<>();

    @Inject(method = "placeRecipe", at = @At("HEAD"), remap = false)
    private void sbjeiindex_addBackpackItems(Recipe<?> recipe, CallbackInfo ci) {
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        if (!handlers.isEmpty()) {
            for (InventoryHandler handler : handlers) {
                if (handler == null) {
                    continue;
                }
                int slots = handler.getSlots();
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        accountStack(stack);
                    }
                }
            }
        }
    }

    @Redirect(
        method = "placeRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
        ),
        remap = false
    )
    private int sbjeiindex_findSlotMatchingItem(net.minecraft.world.entity.player.Inventory inventory, ItemStack stack) {
        int id = inventory.findSlotMatchingItem(stack);
        if (id != -1) {
            return id;
        }
        if (inventory == null || inventory.player == null || stack == null || stack.isEmpty()) {
            return -1;
        }
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(inventory.player);
        if (handlers.isEmpty()) {
            return -1;
        }
        ItemStack[] templates = new ItemStack[]{stack};
        if (BackpackExtraction.containsAnyTemplateMatch(handlers, templates)) {
            BACKPACK_TEMPLATE.set(stack);
            return BACKPACK_SLOT_ID;
        }
        return -1;
    }

    @Redirect(
        method = "placeRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
        ),
        remap = false
    )
    private ItemStack sbjeiindex_removeItem(net.minecraft.world.entity.player.Inventory inventory, int slot, int count) {
        if (slot != BACKPACK_SLOT_ID) {
            return inventory.removeItem(slot, count);
        }
        if (inventory == null || inventory.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack template = BACKPACK_TEMPLATE.get();
        BACKPACK_TEMPLATE.remove();
        if (template == null || template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(inventory.player);
        if (handlers.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return BackpackExtraction.extractFirstTemplateMatch(handlers, new ItemStack[]{template});
    }
}
