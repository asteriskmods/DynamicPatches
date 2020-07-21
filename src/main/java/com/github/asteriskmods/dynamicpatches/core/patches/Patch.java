package com.github.asteriskmods.dynamicpatches.core.patches;

import org.objectweb.asm.tree.ClassNode;

public interface Patch {

    ClassNode install(ClassNode classNode);

}
