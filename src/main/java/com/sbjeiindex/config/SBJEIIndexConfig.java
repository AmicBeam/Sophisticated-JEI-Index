package com.sbjeiindex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SBJEIIndexConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue maxEnabledBackpacksScanned;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("general");
        maxEnabledBackpacksScanned = b
            .comment("Maximum number of equipped backpacks with JEI Index Upgrade to scan (in Sophisticated Backpacks selection order). 0 means unlimited.")
            .defineInRange("maxEnabledBackpacksScanned", 0, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    private SBJEIIndexConfig() {
    }
}
