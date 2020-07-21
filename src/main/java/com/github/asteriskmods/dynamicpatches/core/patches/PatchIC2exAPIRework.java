package com.github.asteriskmods.dynamicpatches.core.patches;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PatchIC2exAPIRework implements Patch {

    public static final PatchIC2exAPIRework INSTANCE = new PatchIC2exAPIRework();

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public ClassNode install(ClassNode classNode) {
        // Generic Patch
        List<MethodNode> constructors = classNode.methods.stream()
            .filter(m -> "<init>".equals(m.name))
            .collect(Collectors.toList());
        Set<String> descriptors = constructors.stream().map(m -> m.desc).collect(Collectors.toSet());
        for (MethodNode method : constructors) {
            final String descriptor = method.desc;
            if (descriptor.contains("Lic2/core/block/IInventorySlotHolder;")) {
                String proxyDesc = descriptor.replace("Lic2/core/block/IInventorySlotHolder;", "Lic2/core/block/TileEntityInventory;");
                LOGGER.debug("Proxy Descriptor is {}", proxyDesc);

                if (descriptors.contains(proxyDesc)) {
                    // has old-style constructor
                    continue;
                }

                MethodVisitor init = classNode.visitMethod(Opcodes.ACC_PUBLIC,
                    "<init>",
                    proxyDesc,
                    null, null
                );
                createDetour(classNode, method, init);
                init.visitEnd();
            } else {
                LOGGER.debug("Skipping descriptor {}", descriptor);
            }
        }

        return classNode;
    }

    private static void createDetour(ClassNode owner, MethodNode target, MethodVisitor visitor) {
        int index = 0;
        // load "this" object
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        for (Type argument : Type.getArgumentTypes(target.desc)) {
            ++index;
            char argumentType = argument.getDescriptor().charAt(0);
            switch (argumentType) {
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                case 'I':
                    visitor.visitVarInsn(Opcodes.ILOAD, index);
                    break;
                case 'J':
                    visitor.visitVarInsn(Opcodes.LLOAD, index);
                    break;
                case 'F':
                    visitor.visitVarInsn(Opcodes.FLOAD, index);
                    break;
                case 'D':
                    visitor.visitVarInsn(Opcodes.DLOAD, index);
                    break;
                case 'L':
                case '[':
                    visitor.visitVarInsn(Opcodes.ALOAD, index);
                    break;
                default:
                    LOGGER.warn("Illegal argument type {} found. falling back to ALOAD...", argumentType);
                    visitor.visitVarInsn(Opcodes.ALOAD, index);
            }
        }
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.name, "<init>", target.desc, false);
        visitor.visitInsn(Opcodes.RETURN);
    }

}
