package me.youded.fastplace;

import java.lang.instrument.Instrumentation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class Agent {
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classfileBuffer == null || classfileBuffer.length == 0) {
                    return new byte[0];
                }

                if (!className.startsWith("net/minecraft")) {
                    return classfileBuffer;
                }

                ClassReader cr = new ClassReader(classfileBuffer);
                if (cr.getInterfaces().length == 3 && "java/lang/Object".equals(cr.getSuperName())) {
                    ClassNode cn = new ClassNode();

                    cr.accept(cn, 0);

                    for (MethodNode method : cn.methods) {
                        boolean hasString = Arrays.stream(method.instructions.toArray())
                                .filter(LdcInsnNode.class::isInstance)
                                .map(LdcInsnNode.class::cast)
                                .map(inst -> inst.cst)
                                .anyMatch("Null returned as 'hitResult', this shouldn't happen!"::equals);
                        if (hasString) {
                            for (AbstractInsnNode insn : method.instructions) {
                                if (insn.getOpcode() == Opcodes.ICONST_4) {
                                    method.instructions.set(insn, new InsnNode(Opcodes.ICONST_0));
                                    ClassWriter cw = new ClassWriter(cr, 0);
                                    cn.accept(cw);
                                    return cw.toByteArray();
                                }
                            }
                        }
                    }
                }

                return classfileBuffer;
            }
        });
    }
}