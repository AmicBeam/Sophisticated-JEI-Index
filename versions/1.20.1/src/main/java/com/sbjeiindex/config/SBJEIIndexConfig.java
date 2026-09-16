package com.sbjeiindex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SBJEIIndexConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue maxEnabledBackpacksScanned;
    public static final ForgeConfigSpec.BooleanValue enableTransferWithoutUpgrade;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        enableTransferWithoutUpgrade = b
            .comment(
                "When true, all equipped backpacks can be used as recipe transfer sources without requiring the JEI Index Upgrade. Existing upgrade items remain registered, but the upgrade is hidden from the creative tab and JEI ingredient list. Client and server should use the same value."
            )
            .define("enableTransferWithoutUpgrade", false);
        maxEnabledBackpacksScanned = b
            .comment("Maximum number of equipped backpacks with JEI Index Upgrade to scan (in Sophisticated Backpacks selection order). 0 means unlimited.")
            .defineInRange("maxEnabledBackpacksScanned", 0, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    private SBJEIIndexConfig() {
    }
}
