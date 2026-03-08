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
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JEICraftingTransferHandler implements IRecipeTransferHandler<CraftingMenu, CraftingRecipe> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IStackHelper stackHelper;
    private final IRecipeTransferHandler<CraftingMenu, CraftingRecipe> defaultHandler;

    public JEICraftingTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
        this.transferHelper = transferHelper;
        this.stackHelper = stackHelper;
        
        // 创建默认的处理器，用于在没有背包时使用
        IRecipeTransferInfo<CraftingMenu, CraftingRecipe> transferInfo = transferHelper.createBasicRecipeTransferInfo(
            CraftingMenu.class, 
            MenuType.CRAFTING, 
            RecipeTypes.CRAFTING, 
            0, 9, 9, 27
        );
        this.defaultHandler = transferHelper.createUnregisteredRecipeTransferHandler(transferInfo);
        
        LOGGER.info("JEICraftingTransferHandler initialized with default handler");
    }

    @Override
    public Class<CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(CraftingMenu container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        LOGGER.info("Transferring recipe to crafting menu for player: {}, maxTransfer: {}, doTransfer: {}", player.getName(), maxTransfer, doTransfer);
        
        // 获取装有 JEI 索引升级的背包
        Object backpackWrapper = BackpackHelper.getEquippedBackpackWithJEIIndexUpgrade(player);
        
        // 如果没有背包，使用默认处理器
        if (backpackWrapper == null) {
            LOGGER.info("No backpack with JEI index upgrade found, using default handler");
            return defaultHandler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
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
            Class<?> backpackWrapperClass = backpackWrapper.getClass();
            java.lang.reflect.Method getInventoryHandlerMethod = backpackWrapperClass.getMethod("getInventoryHandler");
            Object inventoryHandler = getInventoryHandlerMethod.invoke(backpackWrapper);
            
            if (inventoryHandler instanceof IItemHandlerModifiable) {
                IItemHandlerModifiable itemHandler = (IItemHandlerModifiable) inventoryHandler;
                
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
            return defaultHandler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
        }

        // 获取合成槽
        List<Slot> craftingSlots = container.slots.subList(1, 10);
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).stream().toList();

        // 检查是否有足够的空间进行物品转移
        int filledCraftSlotCount = 0;
        int emptySlotCount = 0;
        
        for (Slot slot : craftingSlots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                filledCraftSlotCount++;
            } else {
                emptySlotCount++;
            }
        }

        // 检查是否有足够的空槽位来重新排列物品
        int inputCount = inputSlots.size();
        if (filledCraftSlotCount - inputCount > emptySlotCount) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full");
            return transferHelper.createUserErrorWithTooltip(message);
        }

        // 使用 JEI 的工具来计算转移操作
        RecipeTransferOperationsResult operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                inputSlots,
                craftingSlots
        );

        if (!operations.missingItems.isEmpty()) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
            return transferHelper.createUserErrorForMissingSlots(message, operations.missingItems);
        }

        // 验证槽位
        if (!RecipeTransferUtil.validateSlots(player, operations.results, craftingSlots, new ArrayList<>(availableItemStacks.keySet()))) {
            return transferHelper.createInternalError();
        }

        // 如果只是预览（doTransfer 为 false），返回 null 启用按钮
        if (!doTransfer) {
            LOGGER.info("Recipe transfer preview successful");
            return null;
        }

        // 执行实际的物品转移 - 发送网络包到服务端
        LOGGER.info("Sending recipe transfer packet to server for recipe: {}", recipe.getId());
        
        // 收集配方槽位索引 - 使用输入槽的顺序
        List<Integer> recipeList = new ArrayList<>();
        for (int i = 0; i < inputSlots.size() && i < craftingSlots.size(); i++) {
            recipeList.add(i);
        }

        // 发送网络包到服务端
        NetworkHandler.CHANNEL.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, maxTransfer));

        LOGGER.info("Recipe transfer packet sent for recipe: {}", recipe.getId());
        return null;
    }
}
