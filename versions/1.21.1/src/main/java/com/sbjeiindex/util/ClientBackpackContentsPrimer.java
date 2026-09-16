package com.sbjeiindex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Primes linked backpack snapshots before JEI asks whether a transfer can be performed. */
public final class ClientBackpackContentsPrimer {
    private static final int PRIME_INTERVAL_TICKS = 20;
    private static int ticksUntilPrime;

    private ClientBackpackContentsPrimer() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticksUntilPrime > 0) {
            ticksUntilPrime--;
            return;
        }
        ticksUntilPrime = PRIME_INTERVAL_TICKS;

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            BackpackHelper.primeClientBackpackContents(player);
        }
    }
}
