package com.sbjeiindex.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class SBJEIIndexEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        // Standard handlers are extended uniformly by the EMI compatibility mixins.
    }
}
