package com.sbjeiindex.jei;

import com.sbjeiindex.config.SBJEIIndexConfig;

public final class JeiTransferConstants {
    public static final int BACKPACK_SLOT_ID_OFFSET = 10_000;

    private JeiTransferConstants() {
    }

    public static int getBackpackSlotIdStride() {
        return SBJEIIndexConfig.backpackSlotIdStride.get();
    }
}
