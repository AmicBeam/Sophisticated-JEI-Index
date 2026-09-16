package com.sbjeiindex.emi;

import com.sbjeiindex.util.BackpackHelper;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public final class EmiInputSourcesHelper {
    private EmiInputSourcesHelper() {
    }

    public static <T extends AbstractContainerMenu> List<Slot> extend(StandardRecipeHandler<T> handler, T menu) {
        List<Slot> sources = handler.getInputSources(menu);
        if (!EmiClient.onServer) {
            return sources;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return sources;
        }

        if (BackpackHelper.isBackpackMenu(menu)) {
            return sources;
        }

        List<Slot> extra = EmiBackpackSlots.create(player);
        if (extra.isEmpty()) {
            return sources;
        }

        List<Slot> combined = new ArrayList<>(sources.size() + extra.size());
        combined.addAll(sources);
        combined.addAll(extra);
        return combined;
    }
}
