package com.github.asteriskmods.dynamicpatches.core;

import com.github.asteriskmods.dynamicpatches.core.patches.Patch;
import com.github.asteriskmods.dynamicpatches.core.patches.PatchIC2exAPIRework;
import com.google.common.collect.ImmutableMap;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

public class PatcherClassTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, Patch> PATCHES = ImmutableMap.<String, Patch>builder().build();

    private static byte[] applyPatch(String transformedName, byte[] basicClass, Patch patch) {
        LOGGER.info("Patching class " + transformedName + "...");
        ClassReader reader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        if (patch.install(classNode) == null) {
            return basicClass;
        }
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] applySpecialPatches(String name, String transformedName, byte[] basicClass) {
        if (transformedName.startsWith("ic2.core")) {
            basicClass = applyPatch(transformedName, basicClass, PatchIC2exAPIRework.INSTANCE);
        }
        return basicClass;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        basicClass = applySpecialPatches(name, transformedName, basicClass);
        Patch patch = PATCHES.get(transformedName);
        if (patch != null) {
            applyPatch(transformedName, basicClass, patch);
        }
        return basicClass;
    }

}
