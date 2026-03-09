package com.sbjeiindex.util;

import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static Object getEquippedBackpackWithJEIIndexUpgrade(Player player) {
        LOGGER.info("Checking for equipped backpack with JEI index upgrade for player: {}", player.getName());

        // 检查胸甲槽
        Object chestBackpack = getBackpackFromChestSlot(player);
        if (chestBackpack != null && hasJEIIndexUpgrade(chestBackpack)) {
            LOGGER.info("Found JEI index upgrade in chest slot backpack");
            return chestBackpack;
        }

        // 检查 Curios 槽
        Object curiosBackpack = getBackpackFromCurios(player);
        if (curiosBackpack != null && hasJEIIndexUpgrade(curiosBackpack)) {
            LOGGER.info("Found JEI index upgrade in Curios backpack");
            return curiosBackpack;
        }

        LOGGER.info("No equipped backpack with JEI index upgrade found");
        return null;
    }

    @Nullable
    private static Object getBackpackFromCurios(Player player) {
        LOGGER.info("Checking Curios slot for backpack");
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory", Player.class);
            Object curiosInventoryOptional = getCuriosInventoryMethod.invoke(null, player);

            if (!(curiosInventoryOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object curiosInventory = optional.get();

            Method getCuriosMethod;
            try {
                getCuriosMethod = curiosInventory.getClass().getMethod("getCurios");
            } catch (NoSuchMethodException e) {
                LOGGER.info("Curios inventory does not have getCurios method");
                return null;
            }

            Object curiosMapObj = getCuriosMethod.invoke(curiosInventory);
            if (!(curiosMapObj instanceof Map<?, ?> curiosMap) || curiosMap.isEmpty()) {
                return null;
            }

            List<String> slotTypes = curiosMap.keySet().stream()
                .map(String::valueOf)
                .sorted()
                .toList();

            for (String slotType : slotTypes) {
                Object handler = curiosMap.get(slotType);
                if (handler == null) {
                    continue;
                }

                Object stacksHandler = null;
                for (String methodName : List.of("getStacks", "getStacksHandler", "getInventory")) {
                    try {
                        Method m = handler.getClass().getMethod(methodName);
                        stacksHandler = m.invoke(handler);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                if (stacksHandler == null) {
                    continue;
                }

                Method getSlotsMethod;
                Method getStackInSlotMethod;
                try {
                    getSlotsMethod = stacksHandler.getClass().getMethod("getSlots");
                    getStackInSlotMethod = stacksHandler.getClass().getMethod("getStackInSlot", int.class);
                } catch (NoSuchMethodException e) {
                    continue;
                }

                int slots = ((Number) getSlotsMethod.invoke(stacksHandler)).intValue();
                for (int i = 0; i < slots; i++) {
                    Object stackObj = getStackInSlotMethod.invoke(stacksHandler, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        Object wrapper = getBackpackWrapperFromStack(stack);
                        if (wrapper != null && hasJEIIndexUpgrade(wrapper)) {
                            return wrapper;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("Error checking Curios for backpack: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        return null;
    }

    @Nullable
    private static Object getBackpackFromChestSlot(Player player) {
        ItemStack chestStack = player.getInventory().getArmor(2); // 胸甲槽是索引 2
        LOGGER.info("Checking chest slot for backpack: {}", chestStack);

        if (chestStack.isEmpty()) {
            LOGGER.info("Chest slot is empty");
            return null;
        }

        return getBackpackWrapperFromStack(chestStack);
    }

    @Nullable
    private static Object getBackpackWrapperFromStack(ItemStack stack) {
        try {
            // 使用反射获取 CapabilityBackpackWrapper
            Class<?> capabilityBackpackWrapperClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper");
            Object backpackWrapperCapability = capabilityBackpackWrapperClass.getField("BACKPACK_WRAPPER_CAPABILITY").get(null);
            LOGGER.info("Got BACKPACK_WRAPPER_CAPABILITY: {}", backpackWrapperCapability);

            // 使用反射获取 Capability 类
            Class<?> capabilityClass = Class.forName("net.minecraftforge.common.capabilities.Capability");

            // 使用反射调用 getCapability
            Method getCapabilityMethod = ItemStack.class.getMethod("getCapability", capabilityClass, net.minecraft.core.Direction.class);
            LOGGER.info("Found getCapability method");

            // 调用getCapability方法
            Object result = getCapabilityMethod.invoke(stack, backpackWrapperCapability, null);
            LOGGER.info("getCapability result type: {}", result != null ? result.getClass().getName() : "null");

            // 检查结果是否是 LazyOptional
            if (result != null && result.getClass().getName().contains("LazyOptional")) {
                // 使用反射调用 resolve() 方法获取实际对象
                Method resolveMethod = result.getClass().getMethod("resolve");
                Object optional = resolveMethod.invoke(result);
                LOGGER.info("Resolved LazyOptional type: {}, value: {}", optional != null ? optional.getClass().getName() : "null", optional);

                if (optional != null) {
                    // optional 是 java.util.Optional，需要进一步处理
                    if (optional instanceof Optional<?>) {
                        Optional<?> javaOptional = (Optional<?>) optional;
                        Object value = javaOptional.orElse(null);
                        LOGGER.info("Resolved Optional value type: {}, value: {}", value != null ? value.getClass().getName() : "null", value);
                        return value;
                    }
                    LOGGER.info("Resolved value is not Optional, returning: {}", optional);
                    return optional;
                }
                LOGGER.info("Resolved LazyOptional is null");
                return null;
            }

            // 检查结果是否是 java.util.Optional
            if (result instanceof Optional<?>) {
                Optional<?> optional = (Optional<?>) result;
                Object value = optional.orElse(null);
                LOGGER.info("Optional result: {}", value);
                return value;
            }

            LOGGER.info("Result is not LazyOptional or Optional, returning: {}", result);
            return result;
        } catch (Exception e) {
            LOGGER.error("Error getting backpack from chest slot: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static boolean hasJEIIndexUpgrade(Object backpackWrapper) {
        LOGGER.info("Checking if backpack has JEI index upgrade, backpack type: {}", backpackWrapper.getClass().getName());

        try {
            // 使用反射获取 UpgradeHandler
            Method getUpgradeHandlerMethod = backpackWrapper.getClass().getMethod("getUpgradeHandler");
            Object upgradeHandler = getUpgradeHandlerMethod.invoke(backpackWrapper);
            LOGGER.info("Got upgrade handler: {}, type: {}", upgradeHandler, upgradeHandler.getClass().getName());

            // 获取 UpgradeType 类
            Class<?> upgradeTypeClass = Class.forName("net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType");

            // 打印 JEIIndexUpgradeItem.TYPE 的信息
            LOGGER.info("JEI Index Upgrade TYPE: {}", JEIIndexUpgradeItem.TYPE);
            LOGGER.info("JEI Index Upgrade TYPE class: {}", JEIIndexUpgradeItem.TYPE.getClass().getName());

            // 尝试使用 getTypeWrappers 方法
            try {
                Method getTypeWrappersMethod = upgradeHandler.getClass().getMethod("getTypeWrappers", upgradeTypeClass);
                LOGGER.info("Found getTypeWrappers method with UpgradeType parameter");

                Object typeWrappers = getTypeWrappersMethod.invoke(upgradeHandler, JEIIndexUpgradeItem.TYPE);
                LOGGER.info("Got type wrappers: {}", typeWrappers);

                // 使用 List 接口的 isEmpty 方法而不是反射
                if (typeWrappers instanceof java.util.List<?>) {
                    @SuppressWarnings("unchecked")
                    java.util.List<?> typeWrappersList = (java.util.List<?>) typeWrappers;
                    boolean isEmpty = typeWrappersList.isEmpty();
                    LOGGER.info("Backpack has JEI index upgrade: {}", !isEmpty);
                    return !isEmpty;
                }

                // 备用方法：使用反射
                try {
                    Method isEmptyMethod = typeWrappers.getClass().getMethod("isEmpty");
                    boolean isEmpty = (boolean) isEmptyMethod.invoke(typeWrappers);
                    LOGGER.info("Backpack has JEI index upgrade (reflect): {}", !isEmpty);
                    return !isEmpty;
                } catch (Exception e) {
                    LOGGER.warn("Could not check isEmpty via reflection: {}", e.getMessage());
                }
            } catch (NoSuchMethodException e) {
                LOGGER.info("getTypeWrappers method not found with UpgradeType parameter");
            }

        } catch (Exception e) {
            LOGGER.error("Error checking JEI index upgrade: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }

        return false;
    }
}
