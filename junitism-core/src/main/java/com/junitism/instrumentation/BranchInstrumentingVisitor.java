package com.junitism.instrumentation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that inserts branch distance probes before each branch instruction.
 */
public class BranchInstrumentingVisitor extends MethodVisitor {

    private static final String BRANCH_TRACKER = "com/junitism/runtime/BranchTracker";

    private final String className;
    private final String methodName;
    private int branchCounter = 0;

    public BranchInstrumentingVisitor(int api, MethodVisitor mv, String className, String methodName) {
        super(api, mv);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        int branchId = branchCounter++;

        if (isUnaryIntComparison(opcode)) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(branchId);
            mv.visitLdcInsn(opcode);
            mv.visitLdcInsn(className + "#" + methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRANCH_TRACKER,
                    "trackUnaryBranch", "(IIILjava/lang/String;)V", false);
        } else if (isBinaryIntComparison(opcode)) {
            mv.visitInsn(Opcodes.DUP2);
            mv.visitLdcInsn(branchId);
            mv.visitLdcInsn(opcode);
            mv.visitLdcInsn(className + "#" + methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRANCH_TRACKER,
                    "trackBinaryBranch", "(IIIILjava/lang/String;)V", false);
        } else if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(branchId);
            mv.visitLdcInsn(opcode);
            mv.visitLdcInsn(className + "#" + methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRANCH_TRACKER,
                    "trackNullBranch", "(Ljava/lang/Object;IILjava/lang/String;)V", false);
        } else if (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE) {
            mv.visitInsn(Opcodes.DUP2);
            mv.visitLdcInsn(branchId);
            mv.visitLdcInsn(opcode);
            mv.visitLdcInsn(className + "#" + methodName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRANCH_TRACKER,
                    "trackRefBranch", "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/String;)V", false);
        }
        // TABLESWITCH and LOOKUPSWITCH: we could add probes but they're more complex; skip for now

        super.visitJumpInsn(opcode, label);
    }

    private static boolean isUnaryIntComparison(int opcode) {
        return opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE;
    }

    private static boolean isBinaryIntComparison(int opcode) {
        return opcode >= Opcodes.IF_ICMPEQ && opcode <= Opcodes.IF_ICMPLE;
    }
}
