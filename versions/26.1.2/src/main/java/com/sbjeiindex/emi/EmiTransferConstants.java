package com.sbjeiindex.emi;

import com.sbjeiindex.config.SBJEIIndexConfig;

public final class EmiTransferConstants {
    public static final int BACKPACK_SLOT_ID_OFFSET = 10_000;

    private EmiTransferConstants() {
    }

    public static int getBackpackSlotIdStride() {
        return SBJEIIndexConfig.backpackSlotIdStride.get();
    }
}
