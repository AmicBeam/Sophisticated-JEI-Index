package com.sbjeiindex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SBJEIIndexConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue maxEnabledBackpacksScanned;
    public static final ForgeConfigSpec.BooleanValue enableEmi;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        maxEnabledBackpacksScanned = b
            .comment("Maximum number of equipped backpacks with JEI Index Upgrade to scan (in Sophisticated Backpacks selection order). 0 means unlimited.")
            .defineInRange("maxEnabledBackpacksScanned", 0, 0, 64);
        enableEmi = b.define("enableEmi", true);
        b.pop();

        SPEC = b.build();
    }

    private SBJEIIndexConfig() {
    }
}
