package com.sbjeiindex.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class SBJEIIndexMixinPlugin implements IMixinConfigPlugin {
    private boolean jeiInternalsPresent;

    @Override
    public void onLoad(String mixinPackage) {
        jeiInternalsPresent = hasClass("mezz.jei.common.network.ServerPacketData");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!hasClass(targetClassName)) {
            return false;
        }
        if (!jeiInternalsPresent && mixinClassName.startsWith("com.sbjeiindex.mixin.")) {
            if (mixinClassName.endsWith("BasicRecipeTransferHandlerMixin")
                || mixinClassName.endsWith("PacketRecipeTransferMixin")
                || mixinClassName.endsWith("TransferOperationMixin")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, SBJEIIndexMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
