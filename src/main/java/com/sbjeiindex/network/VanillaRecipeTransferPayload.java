package com.sbjeiindex.network;

import com.sbjeiindex.SBJEIIndex;
import com.sbjeiindex.transfer.VanillaCraftingTransferService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record VanillaRecipeTransferPayload(int containerId, ResourceKey<Recipe<?>> recipeId,
                                           List<ItemStack> templates, boolean maxTransfer,
                                           int requestedSets, int action)
    implements CustomPacketPayload {
    public static final Type<VanillaRecipeTransferPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(SBJEIIndex.MOD_ID, "vanilla_recipe_transfer")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, VanillaRecipeTransferPayload> STREAM_CODEC =
        StreamCodec.of(VanillaRecipeTransferPayload::encode, VanillaRecipeTransferPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, VanillaRecipeTransferPayload payload) {
        buffer.writeVarInt(payload.containerId);
        ResourceKey.streamCodec(Registries.RECIPE).encode(buffer, payload.recipeId);
        buffer.writeBoolean(payload.maxTransfer);
        buffer.writeVarInt(payload.requestedSets);
        buffer.writeByte(payload.action);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < payload.templates.size() ? payload.templates.get(i) : ItemStack.EMPTY;
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        }
    }

    private static VanillaRecipeTransferPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        ResourceKey<Recipe<?>> recipeId = ResourceKey.streamCodec(Registries.RECIPE).decode(buffer);
        boolean maxTransfer = buffer.readBoolean();
        int requestedSets = buffer.readVarInt();
        int action = buffer.readUnsignedByte();
        List<ItemStack> templates = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            templates.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
        return new VanillaRecipeTransferPayload(containerId, recipeId, List.copyOf(templates), maxTransfer, requestedSets, action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VanillaRecipeTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                VanillaCraftingTransferService.transfer(player, payload);
            }
        });
    }
}
