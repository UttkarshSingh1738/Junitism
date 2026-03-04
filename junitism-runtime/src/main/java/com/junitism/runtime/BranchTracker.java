package com.junitism.runtime;

/**
 * Branch distance probe receiver. Thread-local for parallel test execution.
 * Instrumented code calls these methods; the core engine reads the data.
 */
public final class BranchTracker {

    private static final ThreadLocal<BranchExecutionData> currentExecution =
            ThreadLocal.withInitial(BranchExecutionData::new);

    private BranchTracker() {}

    public static void reset() {
        currentExecution.set(new BranchExecutionData());
    }

    public static BranchExecutionData getAndReset() {
        BranchExecutionData data = currentExecution.get();
        currentExecution.set(new BranchExecutionData());
        return data;
    }

    public static void trackUnaryBranch(int value, int branchId, int opcode, String location) {
        double trueDist = computeUnaryTrueDistance(value, opcode);
        double falseDist = computeUnaryFalseDistance(value, opcode);
        currentExecution.get().record(branchId, location, trueDist, falseDist, value, 0);
    }

    public static void trackBinaryBranch(int val1, int val2, int branchId, int opcode, String location) {
        double trueDist = computeBinaryTrueDistance(val1, val2, opcode);
        double falseDist = computeBinaryFalseDistance(val1, val2, opcode);
        currentExecution.get().record(branchId, location, trueDist, falseDist, val1, val2);
    }

    public static void trackNullBranch(Object ref, int branchId, int opcode, String location) {
        double trueDist = ref == null ? 0 : 1;
        double falseDist = ref == null ? 1 : 0;
        currentExecution.get().record(branchId, location, trueDist, falseDist, 0, 0);
    }

    public static void trackRefBranch(Object ref1, Object ref2, int branchId, int opcode, String location) {
        boolean same = ref1 == ref2;
        double trueDist = (opcode == 166) ? (same ? 0 : 1) : (same ? 1 : 0); // IF_ACMPEQ=166, IF_ACMPNE=167
        double falseDist = (opcode == 166) ? (same ? 1 : 0) : (same ? 0 : 1);
        currentExecution.get().record(branchId, location, trueDist, falseDist, 0, 0);
    }

    private static double computeUnaryTrueDistance(int value, int opcode) {
        return switch (opcode) {
            case 153 -> value == 0 ? 0 : 1;  // IFEQ
            case 154 -> value != 0 ? 0 : 1;  // IFNE
            case 155 -> value < 0 ? 0 : value + 1;   // IFLT
            case 156 -> value >= 0 ? 0 : -value;     // IFGE
            case 157 -> value > 0 ? 0 : 1 - value;  // IFGT
            case 158 -> value <= 0 ? 0 : value;     // IFLE
            default -> 0;
        };
    }

    private static double computeUnaryFalseDistance(int value, int opcode) {
        return switch (opcode) {
            case 153 -> value == 0 ? 1 : 0;  // IFEQ
            case 154 -> value != 0 ? 1 : 0;  // IFNE
            case 155 -> value >= 0 ? 0 : value + 1;  // IFLT
            case 156 -> value < 0 ? 0 : value;       // IFGE
            case 157 -> value <= 0 ? 0 : value - 1;  // IFGT
            case 158 -> value > 0 ? 0 : 1 - value;   // IFLE
            default -> 0;
        };
    }

    private static double computeBinaryTrueDistance(int a, int b, int opcode) {
        return switch (opcode) {
            case 159 -> a == b ? 0 : Math.abs(a - b);           // IF_ICMPEQ
            case 160 -> a != b ? 0 : (a == b ? 1 : 0);          // IF_ICMPNE
            case 161 -> a < b ? 0 : (a - b + 1);                 // IF_ICMPLT
            case 162 -> a >= b ? 0 : (b - a);                    // IF_ICMPGE
            case 163 -> a > b ? 0 : (b - a + 1);                 // IF_ICMPGT
            case 164 -> a <= b ? 0 : (a - b);                    // IF_ICMPLE
            default -> 0;
        };
    }

    private static double computeBinaryFalseDistance(int a, int b, int opcode) {
        return switch (opcode) {
            case 159 -> a == b ? 1 : 0;                          // IF_ICMPEQ
            case 160 -> a != b ? 0 : Math.abs(a - b);            // IF_ICMPNE
            case 161 -> a >= b ? 0 : (b - a);                    // IF_ICMPLT
            case 162 -> a < b ? 0 : (a - b + 1);                 // IF_ICMPGE
            case 163 -> a <= b ? 0 : (a - b);                    // IF_ICMPGT
            case 164 -> a > b ? 0 : (b - a + 1);                 // IF_ICMPLE
            default -> 0;
        };
    }
}
