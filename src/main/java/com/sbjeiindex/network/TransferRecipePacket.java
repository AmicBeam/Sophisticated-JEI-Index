package com.sbjeiindex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TransferRecipePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ResourceLocation recipeId;
    private final List<Integer> recipeList;
    private final boolean maxTransfer;

    public TransferRecipePacket(ResourceLocation recipeId, List<Integer> recipeList, boolean maxTransfer) {
        this.recipeId = recipeId;
        this.recipeList = recipeList;
        this.maxTransfer = maxTransfer;
        LOGGER.info("Created TransferRecipePacket for recipe: {}, maxTransfer: {}", recipeId, maxTransfer);
    }

    public static void encode(TransferRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
        buffer.writeInt(packet.recipeList.size());
        for (Integer index : packet.recipeList) {
            buffer.writeInt(index);
        }
        buffer.writeBoolean(packet.maxTransfer);
        LOGGER.info("Encoded TransferRecipePacket: {}", packet.recipeId);
    }

    public static TransferRecipePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        int size = buffer.readInt();
        List<Integer> recipeList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            recipeList.add(buffer.readInt());
        }
        boolean maxTransfer = buffer.readBoolean();
        LOGGER.info("Decoded TransferRecipePacket: {}", recipeId);
        return new TransferRecipePacket(recipeId, recipeList, maxTransfer);
    }

    public List<Integer> getRecipeList() {
        return recipeList;
    }

    public static void handle(TransferRecipePacket packet, Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            LOGGER.info("Handling TransferRecipePacket for recipe: {}, maxTransfer: {}", packet.recipeId, packet.maxTransfer);
            
            Player player = context.get().getSender();
            if (player == null) {
                LOGGER.error("Player is null, cannot handle recipe transfer");
                return;
            }
            
            // 获取装有 JEI 索引升级的背包
            Object backpackWrapper = com.sbjeiindex.util.BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
            if (backpackWrapper == null) {
                LOGGER.error("No backpack with JEI index upgrade found for player: {}", player.getName());
                return;
            }
            
            // 获取配方
            RecipeManager recipeManager = player.level().getRecipeManager();
            Optional<? extends net.minecraft.world.item.crafting.Recipe<?>> recipeById = recipeManager.byKey(packet.recipeId);
            
            if (recipeById.isEmpty() || !(recipeById.get() instanceof CraftingRecipe recipe)) {
                return;
            }
            
            // 获取合成菜单
            if (!(player.containerMenu instanceof CraftingMenu craftingMenu)) {
                LOGGER.error("Player is not in a crafting menu");
                return;
            }
            
            // 获取背包物品处理器
            IItemHandlerModifiable itemHandler = null;
            try {
                java.lang.reflect.Method getInventoryHandlerMethod = backpackWrapper.getClass().getMethod("getInventoryHandler");
                Object inventoryHandler = getInventoryHandlerMethod.invoke(backpackWrapper);
                if (inventoryHandler instanceof IItemHandlerModifiable) {
                    itemHandler = (IItemHandlerModifiable) inventoryHandler;
                }
            } catch (Exception e) {
                LOGGER.error("Error getting backpack inventory handler: {}", e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // 获取合成槽
            List<Slot> craftingSlots = craftingMenu.slots.subList(1, 10);
            
            // 清除合成网格中的物品
            for (Slot slot : craftingSlots) {
                if (slot.mayPickup(player) && slot.hasItem()) {
                    ItemStack stack = slot.remove(slot.getItem().getCount());
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
            
            // 获取配方材料
            List<Ingredient> ingredients = recipe.getIngredients().stream()
                    .filter(ingredient -> !ingredient.isEmpty())
                    .toList();
            
            // 计算最大可合成次数
            int maxCraftable = 1;
            if (packet.maxTransfer) {
                maxCraftable = calculateMaxCraftable(recipe, player.getInventory(), itemHandler);
                LOGGER.info("Max craftable: {}", maxCraftable);
                if (maxCraftable <= 0) {
                    return;
                }
            }
            
            // 为每个配方材料槽填入物品
            for (int slotIdx = 0; slotIdx < packet.getRecipeList().size() && slotIdx < ingredients.size(); slotIdx++) {
                int craftingSlotIndex = packet.getRecipeList().get(slotIdx);
                if (craftingSlotIndex < 0 || craftingSlotIndex >= craftingSlots.size()) continue;
                
                Slot targetSlot = craftingSlots.get(craftingSlotIndex);
                Ingredient ingredient = ingredients.get(slotIdx);
                
                // 计算需要的材料数量
                int totalNeeded = maxCraftable;
                
                // 从玩家背包取材料
                for (int invIdx = 0; invIdx < player.getInventory().getContainerSize() && totalNeeded > 0; invIdx++) {
                    ItemStack stack = player.getInventory().getItem(invIdx);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        int canTake = Math.min(stack.getCount(), totalNeeded);
                        ItemStack toInsert = stack.split(canTake);
                        totalNeeded -= canTake;
                        
                        if (targetSlot.getItem().isEmpty()) {
                            targetSlot.set(toInsert);
                        } else {
                            targetSlot.getItem().grow(toInsert.getCount());
                        }
                    }
                }
                
                // 从背包取材料
                if (totalNeeded > 0 && itemHandler != null) {
                    for (int backpackIdx = 0; backpackIdx < itemHandler.getSlots() && totalNeeded > 0; backpackIdx++) {
                        ItemStack stack = itemHandler.getStackInSlot(backpackIdx);
                        if (!stack.isEmpty() && ingredient.test(stack)) {
                            int canTake = Math.min(stack.getCount(), totalNeeded);
                            ItemStack toInsert = stack.split(canTake);
                            totalNeeded -= canTake;
                            
                            itemHandler.setStackInSlot(backpackIdx, stack);
                            
                            if (targetSlot.getItem().isEmpty()) {
                                targetSlot.set(toInsert);
                            } else {
                                targetSlot.getItem().grow(toInsert.getCount());
                            }
                        }
                    }
                }
            }
            
            LOGGER.info("Recipe transfer completed for player: {}, recipe: {}", player.getName(), packet.recipeId);
        });
        context.get().setPacketHandled(true);
    }
    
    private static int calculateMaxCraftable(CraftingRecipe recipe, net.minecraft.world.entity.player.Inventory playerInv, IItemHandlerModifiable itemHandler) {
        List<Ingredient> ingredients = recipe.getIngredients().stream()
                .filter(ingredient -> !ingredient.isEmpty())
                .toList();
        
        if (ingredients.isEmpty()) {
            return 0;
        }
        
        int minAvailable = Integer.MAX_VALUE;
        
        for (Ingredient ingredient : ingredients) {
            int available = 0;
            
            for (int i = 0; i < playerInv.getContainerSize(); i++) {
                ItemStack stack = playerInv.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    available += stack.getCount();
                }
            }
            
            if (itemHandler != null) {
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        available += stack.getCount();
                    }
                }
            }
            
            if (available == 0) {
                return 0;
            }
            
            minAvailable = Math.min(minAvailable, available);
        }
        
        return minAvailable;
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public boolean isMaxTransfer() {
        return maxTransfer;
    }
}
