package com.sbjeiindex.jei;

import com.sbjeiindex.network.TransferRecipePacket;
import com.sbjeiindex.network.NetworkHandler;
import com.sbjeiindex.util.BackpackHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JEIStonecuttingTransferHandler implements IRecipeTransferHandler<StonecutterMenu, StonecutterRecipe> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IStackHelper stackHelper;

    public JEIStonecuttingTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
        this.transferHelper = transferHelper;
        this.stackHelper = stackHelper;
        LOGGER.info("JEIStonecuttingTransferHandler initialized");
    }

    @Override
    public Class<StonecutterMenu> getContainerClass() {
        return StonecutterMenu.class;
    }

    @Override
    public RecipeType<StonecutterRecipe> getRecipeType() {
        return RecipeTypes.STONECUTTING;
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.MenuType<StonecutterMenu>> getMenuType() {
        return java.util.Optional.of(net.minecraft.world.inventory.MenuType.STONECUTTER);
    }

    @Override
    public IRecipeTransferError transferRecipe(StonecutterMenu container, StonecutterRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        LOGGER.info("Transferring recipe to stonecutter menu for player: {}, maxTransfer: {}, doTransfer: {}", player.getName(), maxTransfer, doTransfer);
        
        // 获取装有 JEI 索引升级的背包
        Object backpackWrapper = BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
        
        // 如果没有背包，返回 null 让 JEI 使用默认行为
        if (backpackWrapper == null) {
            LOGGER.info("No backpack with JEI index upgrade found, returning null for JEI default handling");
            return null;
        }

        LOGGER.info("Found backpack with JEI index upgrade, extending available items");
        
        Inventory playerInventory = player.getInventory();
        Map<Slot, ItemStack> availableItemStacks = new LinkedHashMap<>();

        // 添加玩家主背包物品
        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!stack.isEmpty()) {
                availableItemStacks.put(new Slot(playerInventory, i, 0, 0), stack);
            }
        }

        // 添加背包物品
        try {
            // 使用反射获取背包的物品处理器
            Class<?> backpackWrapperClass = backpackWrapper.getClass();
            java.lang.reflect.Method getInventoryHandlerMethod = backpackWrapperClass.getMethod("getInventoryHandler");
            Object inventoryHandler = getInventoryHandlerMethod.invoke(backpackWrapper);
            
            // 检查是否是 IItemHandlerModifiable
            if (inventoryHandler instanceof IItemHandlerModifiable) {
                IItemHandlerModifiable itemHandler = (IItemHandlerModifiable) inventoryHandler;
                
                // 添加背包中的物品
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        availableItemStacks.put(new SlotItemHandler(itemHandler, i, 0, 0), stack);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting backpack inventory: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        // 检查配方材料是否齐全
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        Map<Slot, ItemStack> remainingStacks = new LinkedHashMap<>();
        availableItemStacks.forEach((slot, stack) -> remainingStacks.put(slot, stack.copy()));

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews()) {
            if (slotView.getRole() != RecipeIngredientRole.INPUT || slotView.isEmpty()) {
                continue;
            }

            boolean matched = false;

            for (ItemStack displayed : slotView.getItemStacks().toList()) {
                if (displayed.isEmpty()) continue;

                Ingredient ingredient = Ingredient.of(displayed);

                for (Map.Entry<Slot, ItemStack> entry : remainingStacks.entrySet()) {
                    ItemStack testStack = entry.getValue();

                    if (!testStack.isEmpty() && ingredient.test(testStack)) {
                        testStack.shrink(1);
                        if (testStack.isEmpty()) {
                            entry.setValue(ItemStack.EMPTY);
                        }
                        matched = true;
                        break;
                    }
                }

                if (matched) break;
            }

            if (!matched) {
                missingSlots.add(slotView);
            }
        }

        if (!missingSlots.isEmpty()) {
            LOGGER.info("Missing slots found: {}", missingSlots.size());
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
            return transferHelper.createUserErrorForMissingSlots(message, missingSlots);
        }

        // 所有材料都找到了：启用按钮
        if (!doTransfer) {
            LOGGER.info("Recipe transfer preview successful");
            return null;
        }

        // 执行实际的物品转移 - 发送网络包到服务端
        LOGGER.info("Sending recipe transfer packet to server for recipe: {}", recipe.getId());
        
        // 计算配方转移操作
        List<Slot> stonecutterSlots = container.slots.subList(1, 2); // 切石机只有一个输入槽

        RecipeTransferOperationsResult operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                recipeSlots.getSlotViews(RecipeIngredientRole.INPUT),
                stonecutterSlots
        );

        if (!operations.missingItems.isEmpty()) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
            return transferHelper.createUserErrorForMissingSlots(message, operations.missingItems);
        }

        // 收集配方列表（槽位索引）
        List<Integer> recipeList = new ArrayList<>();
        recipeList.add(0); // 切石机输入槽索引

        // 发送网络包到服务端
        NetworkHandler.CHANNEL.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, maxTransfer));

        LOGGER.info("Recipe transfer packet sent for recipe: {}", recipe.getId());
        return null;
    }
}
