package com.sbjeiindex.mixin.bd;

import com.sbjeiindex.util.BackpackHelper;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(targets = "com.wintercogs.beyonddimensions.integration.module.emi.recipe.NetRecipeHandler", remap = false)
public class NetRecipeHandlerMixin<T extends DimensionsCraftMenu> {
    @Redirect(
        method = "craft",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Inventory;items:Lnet/minecraft/core/NonNullList;",
            opcode = Opcodes.GETFIELD
        ),
        remap = false
    )
    private NonNullList<ItemStack> sbjeiindex_appendBackpackItems(Inventory inventory) {
        NonNullList<ItemStack> combined = NonNullList.create();
        combined.addAll(inventory.items);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return combined;
        }

        List<InventoryHandler> handlers = BackpackHelper.getEquippedBackpackInventoryHandlersWithJEIIndexUpgrade(player);
        for (InventoryHandler handler : handlers) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    combined.add(stack.copy());
                }
            }
        }

        return combined;
    }
}
