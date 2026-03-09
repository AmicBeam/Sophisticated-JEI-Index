package com.sbjeiindex.util;

import com.sbjeiindex.upgrade.JEIIndexUpgradeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CURIOS_MOD_ID = "curios";
    private static final List<String> CURIOS_HANDLER_STACKS_ACCESSORS = List.of("getStacks", "getStacksHandler", "getInventory");
    private static final ConcurrentHashMap<Class<?>, Method> CURIOS_HANDLER_TO_STACKS_METHOD = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> CURIOS_STACKS_GET_SLOTS_METHOD = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> CURIOS_STACKS_GET_STACK_IN_SLOT_METHOD = new ConcurrentHashMap<>();
    private static volatile boolean curiosApiInitialized = false;
    @Nullable
    private static volatile Method curiosGetCuriosInventoryMethod = null;
    private static volatile boolean curiosInitLogged = false;

    @Nullable
    public static IBackpackWrapper getEquippedBackpackWithJEIIndexUpgrade(Player player) {
        IBackpackWrapper chestBackpack = getBackpackFromChestSlot(player);
        if (chestBackpack != null && hasJEIIndexUpgrade(chestBackpack)) {
            return chestBackpack;
        }

        IBackpackWrapper curiosBackpack = getBackpackFromCurios(player);
        if (curiosBackpack != null && hasJEIIndexUpgrade(curiosBackpack)) {
            return curiosBackpack;
        }

        return null;
    }

    @Nullable
    private static IBackpackWrapper getBackpackFromCurios(Player player) {
        if (!ModList.get().isLoaded(CURIOS_MOD_ID)) {
            return null;
        }

        Method getCuriosInventoryMethod = getCuriosGetCuriosInventoryMethod();
        if (getCuriosInventoryMethod == null) {
            return null;
        }

        try {
            Object curiosInventoryOptional = getCuriosInventoryMethod.invoke(null, player);
            if (!(curiosInventoryOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object curiosInventory = optional.get();
            Method getCuriosMethod = getNoArgMethod(curiosInventory.getClass(), "getCurios");
            if (getCuriosMethod == null) {
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

                Object stacksHandler = getStacksHandler(handler);
                if (stacksHandler == null) {
                    continue;
                }

                Method getSlotsMethod = CURIOS_STACKS_GET_SLOTS_METHOD.computeIfAbsent(stacksHandler.getClass(), c -> getNoArgMethod(c, "getSlots"));
                Method getStackInSlotMethod = CURIOS_STACKS_GET_STACK_IN_SLOT_METHOD.computeIfAbsent(stacksHandler.getClass(), c -> getMethod(c, "getStackInSlot", int.class));
                if (getSlotsMethod == null || getStackInSlotMethod == null) {
                    continue;
                }

                int slots = ((Number) getSlotsMethod.invoke(stacksHandler)).intValue();
                for (int i = 0; i < slots; i++) {
                    Object stackObj = getStackInSlotMethod.invoke(stacksHandler, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        IBackpackWrapper wrapper = getBackpackWrapperFromStack(stack);
                        if (wrapper != null && hasJEIIndexUpgrade(wrapper)) {
                            return wrapper;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking Curios for backpack", e);
        }

        return null;
    }

    @Nullable
    private static Method getCuriosGetCuriosInventoryMethod() {
        if (curiosApiInitialized) {
            return curiosGetCuriosInventoryMethod;
        }
        synchronized (BackpackHelper.class) {
            if (curiosApiInitialized) {
                return curiosGetCuriosInventoryMethod;
            }
            curiosApiInitialized = true;
            try {
                Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                curiosGetCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory", Player.class);
            } catch (Exception e) {
                curiosGetCuriosInventoryMethod = null;
                if (!curiosInitLogged) {
                    curiosInitLogged = true;
                    LOGGER.warn("Curios is loaded but CuriosApi reflection init failed", e);
                }
            }
            return curiosGetCuriosInventoryMethod;
        }
    }

    @Nullable
    private static Object getStacksHandler(Object curiosHandler) {
        Method cached = CURIOS_HANDLER_TO_STACKS_METHOD.get(curiosHandler.getClass());
        if (cached != null) {
            try {
                return cached.invoke(curiosHandler);
            } catch (Exception e) {
                return null;
            }
        }

        for (String methodName : CURIOS_HANDLER_STACKS_ACCESSORS) {
            Method m = getNoArgMethod(curiosHandler.getClass(), methodName);
            if (m == null) {
                continue;
            }
            CURIOS_HANDLER_TO_STACKS_METHOD.put(curiosHandler.getClass(), m);
            try {
                return m.invoke(curiosHandler);
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    private static Method getNoArgMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static Method getMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static IBackpackWrapper getBackpackFromChestSlot(Player player) {
        ItemStack chestStack = player.getInventory().getArmor(2); // 胸甲槽是索引 2
        if (chestStack.isEmpty()) {
            return null;
        }

        return getBackpackWrapperFromStack(chestStack);
    }

    @Nullable
    private static IBackpackWrapper getBackpackWrapperFromStack(ItemStack stack) {
        LazyOptional<IBackpackWrapper> cap = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance());
        return cap.resolve().orElse(null);
    }

    private static boolean hasJEIIndexUpgrade(IBackpackWrapper backpackWrapper) {
        try {
            backpackWrapper.onContentsNbtUpdated();
            return !backpackWrapper.getUpgradeHandler().getTypeWrappers(JEIIndexUpgradeItem.TYPE).isEmpty();
        } catch (Exception e) {
            LOGGER.warn("Error checking JEI index upgrade", e);
            return false;
        }
    }
}
