package com.sbjeiindex.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Shared plugin entry point retained by the optional integration mixin configs. */
public class SBJEIIndexMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!targetClassName.startsWith("mezz.jei.common.network.packets.")) {
            return true;
        }
        // Query bytecode without loading a target before Mixin transforms it.
        try {
            return org.spongepowered.asm.service.MixinService.getService()
                .getBytecodeProvider().getClassNode(targetClassName) != null;
        } catch (ClassNotFoundException | java.io.IOException e) {
            return false;
        }
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
