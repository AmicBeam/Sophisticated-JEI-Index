package com.sbjeiindex.network;

import com.sbjeiindex.transfer.VanillaCraftingTransferService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record VanillaRecipeTransferPayload(int containerId, ResourceLocation recipeId,
                                           List<ItemStack> templates, boolean maxTransfer,
                                           int requestedSets, int action) {
    public static void encode(VanillaRecipeTransferPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeResourceLocation(payload.recipeId);
        buffer.writeBoolean(payload.maxTransfer);
        buffer.writeVarInt(payload.requestedSets);
        buffer.writeByte(payload.action);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < payload.templates.size() ? payload.templates.get(i) : ItemStack.EMPTY;
            buffer.writeItem(stack);
        }
    }

    public static VanillaRecipeTransferPayload decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        ResourceLocation recipeId = buffer.readResourceLocation();
        boolean maxTransfer = buffer.readBoolean();
        int requestedSets = buffer.readVarInt();
        int action = buffer.readUnsignedByte();
        List<ItemStack> templates = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            templates.add(buffer.readItem());
        }
        return new VanillaRecipeTransferPayload(containerId, recipeId, List.copyOf(templates), maxTransfer, requestedSets, action);
    }

    public static void handle(VanillaRecipeTransferPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                VanillaCraftingTransferService.transfer(player, payload);
            }
        });
        context.setPacketHandled(true);
    }
}
