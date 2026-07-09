package com.sbjeiindex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SBJEIIndexConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue maxEnabledBackpacksScanned;
    public static final ModConfigSpec.IntValue backpackSlotIdStride;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("general");
        maxEnabledBackpacksScanned = b
            .comment("Maximum number of equipped backpacks with JEI Index Upgrade to scan (in Sophisticated Backpacks selection order). 0 means unlimited.")
            .defineInRange("maxEnabledBackpacksScanned", 0, 0, 64);
        backpackSlotIdStride = b
            .comment(
                "Virtual slot id stride reserved for each indexed backpack source. Increase this if an Inception Upgrade makes one indexed backpack expose more slots than the default. Client and server must use the same value."
            )
            .defineInRange("backpackSlotIdStride", 100_000, 10_000, 10_000_000);
        b.pop();

        SPEC = b.build();
    }

    private SBJEIIndexConfig() {
    }
}
