package com.junitism.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ClassVisitor that wraps each method with BranchInstrumentingVisitor.
 */
public class BranchInstrumentingClassVisitor extends ClassVisitor {

    private String className;

    public BranchInstrumentingClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                     String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv != null && !"<clinit>".equals(name)) {
            return new BranchInstrumentingVisitor(Opcodes.ASM9, mv, className, name);
        }
        return mv;
    }
}
