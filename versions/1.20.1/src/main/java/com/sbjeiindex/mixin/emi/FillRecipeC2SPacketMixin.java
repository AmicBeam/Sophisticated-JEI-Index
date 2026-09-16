package com.sbjeiindex.mixin.emi;

import com.sbjeiindex.emi.EmiServerSlotResolver;
import com.sbjeiindex.emi.EmiTransferConstants;
import dev.emi.emi.network.FillRecipeC2SPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FillRecipeC2SPacket.class, remap = false)
public class FillRecipeC2SPacketMixin {
    @Shadow @Final private List<Integer> slots;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void sbjeiindex_validateBackpackSources(Player player, CallbackInfo ci) {
        if (slots == null) {
            return;
        }
        int menuSlots = player.containerMenu.slots.size();
        for (int id : slots) {
            if (id >= 0 && id < menuSlots) {
                continue;
            }
            if (EmiServerSlotResolver.resolve(player, id) == null) {
                ci.cancel();
                return;
            }
        }
    }

    @Redirect(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I", ordinal = 0)
    )
    private int sbjeiindex_allowBackpackSourceIds(NonNullList<?> menuSlots) {
        return EmiTransferConstants.BACKPACK_SLOT_ID_OFFSET +
            EmiTransferConstants.BACKPACK_SLOT_ID_STRIDE * 1_000;
    }

    @Redirect(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;", ordinal = 0)
    )
    private Object sbjeiindex_resolveBackpackSource(NonNullList<Slot> menuSlots, int id, Player player) {
        if (id >= 0 && id < menuSlots.size()) {
            return menuSlots.get(id);
        }
        return EmiServerSlotResolver.resolve(player, id);
    }
}
