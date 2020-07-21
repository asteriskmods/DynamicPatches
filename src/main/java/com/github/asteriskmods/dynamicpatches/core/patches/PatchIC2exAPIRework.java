package com.github.asteriskmods.dynamicpatches.core.patches;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.TileEntityInventory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PatchIC2exAPIRework implements Patch {

    public static final PatchIC2exAPIRework INSTANCE = new PatchIC2exAPIRework();

    private static final Logger LOGGER = LogManager.getLogger();

    private static final class CompilationMock {

        public CompilationMock(TileEntityInventory a, String b, int c, IMachineRecipeManager<?, ?, ?> d) {
            this((IInventorySlotHolder<?>) a, b, c, d);
        }

        public CompilationMock(IInventorySlotHolder<?> a, String b, int c, IMachineRecipeManager<?, ?, ?> d) {
        }

    }

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
                LOGGER.info("Proxy Descriptor is {}", proxyDesc);

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
                LOGGER.info("Skipping descriptor {}", descriptor);
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
//                case '[':
//                    char elementType = argument.getDescriptor().charAt(1);
//                    switch (elementType) {
//                        case 'Z':
//                        case 'B':
//                            visitor.visitVarInsn(Opcodes.BALOAD, index);
//                            break;
//                        case 'S':
//                            visitor.visitVarInsn(Opcodes.SALOAD, index);
//                            break;
//                        case 'C':
//                            visitor.visitVarInsn(Opcodes.CALOAD, index);
//                            break;
//                        case 'I':
//                            visitor.visitVarInsn(Opcodes.IALOAD, index);
//                            break;
//                        case 'J':
//                            visitor.visitVarInsn(Opcodes.LALOAD, index);
//                            break;
//                        case 'F':
//                            visitor.visitVarInsn(Opcodes.FALOAD, index);
//                            break;
//                        case 'D':
//                            visitor.visitVarInsn(Opcodes.DALOAD, index);
//                            break;
//                        default:
//                            // Array or Object
//                            visitor.visitVarInsn(Opcodes.AALOAD, index);
//                    }
//                    break;
                default:
                    LOGGER.warn("Illegal argument type {} found. falling back to ALOAD...", argumentType);
                    visitor.visitVarInsn(Opcodes.ALOAD, index);
            }
        }
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.name, "<init>", target.desc, false);
        visitor.visitInsn(Opcodes.RETURN);
    }

    private static ClassNode applyOldPatch(ClassNode classNode) {
        boolean foundRedirectTarget = false;

        for (MethodNode method : classNode.methods) {
            if ("(Lic2/core/block/TileEntityInventory;Ljava/lang/String;ILic2/api/recipe/IMachineRecipeManager;)V".equals(method.desc)) {
                // The class already has old-style constructor. Patch is not required.
                return null;
            }

            if ("(Lic2/core/block/IInventorySlotHolder;Ljava/lang/String;ILic2/api/recipe/IMachineRecipeManager;)V".equals(method.desc)) {
                foundRedirectTarget = true;
            }
        }

        if (!foundRedirectTarget) {
            // Redirect Target is not found. This patch is not supported.
            return null;
        }

        // Create a detour.
        MethodVisitor init = classNode.visitMethod(Opcodes.ACC_PUBLIC,
            "<init>",
            "(Lic2/core/block/TileEntityInventory;Ljava/lang/String;ILic2/api/recipe/IMachineRecipeManager;)V",
            null, null
        );
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ALOAD, 1);
        init.visitVarInsn(Opcodes.ALOAD, 2);
        init.visitVarInsn(Opcodes.ILOAD, 3);
        init.visitVarInsn(Opcodes.ALOAD, 4);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "ic2/core/block/invslot/InvSlotProcessable",
            "<init>", "(Lic2/core/block/IInventorySlotHolder;Ljava/lang/String;ILic2/api/recipe/IMachineRecipeManager;)V",
            false);
        init.visitInsn(Opcodes.RETURN);
        init.visitEnd();

        return classNode;
    }

}
