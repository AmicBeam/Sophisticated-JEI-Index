package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.util.BackpackHelper;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.platform.EmiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EmiPlayerInventory.class, remap = false)
public abstract class EmiPlayerInventoryMixin {
    @Shadow
    private void addStack(ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"))
    private void sbjeiindex_addBackpackStacks(Player player, CallbackInfo ci) {
        if (!EmiClient.onServer) {
            return;
        }
        if (BackpackHelper.isBackpackMenu(player.containerMenu)) {
            return;
        }
        sbjeiindex_addFromBackpacks(player);
    }

    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("TAIL"))
    private void sbjeiindex_addBackpackStacksFromClientPlayer(List<?> stacks, CallbackInfo ci) {
        if (!EmiClient.onServer) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (BackpackHelper.isBackpackMenu(player.containerMenu)) {
            return;
        }
        sbjeiindex_addFromBackpacks(player);
    }

    private void sbjeiindex_addFromBackpacks(Player player) {
        List<IItemHandlerModifiable> backpackHandlers = BackpackHelper.getEquippedBackpackItemHandlersWithJEIIndexUpgrade(player);
        for (IItemHandlerModifiable handler : backpackHandlers) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    addStack(stack);
                }
            }
        }
    }
}
