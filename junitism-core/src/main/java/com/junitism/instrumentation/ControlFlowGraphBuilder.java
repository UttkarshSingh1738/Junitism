package com.junitism.instrumentation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a Control Flow Graph from bytecode (basic blocks, edges, branch IDs).
 */
public class ControlFlowGraphBuilder extends MethodVisitor {

    public static class BasicBlock {
        public final int id;
        public final int startOffset;
        public final List<Integer> branchIds = new ArrayList<>();
        public final Set<BasicBlock> successors = new HashSet<>();
        public final Set<BasicBlock> predecessors = new HashSet<>();

        BasicBlock(int id, int startOffset) {
            this.id = id;
            this.startOffset = startOffset;
        }
    }

    private final List<BasicBlock> blocks = new ArrayList<>();
    private final Map<Label, BasicBlock> blockByLabel = new HashMap<>();
    private final Map<Integer, BasicBlock> blockByBranchId = new HashMap<>();
    private final Set<Label> jumpTargets = new HashSet<>();
    private final List<Object[]> pendingEdges = new ArrayList<>();
    private BasicBlock currentBlock;
    private int blockIdCounter = 0;
    private int branchIdCounter = 0;
    private int offset = 0;

    public ControlFlowGraphBuilder(String className, String methodName) {
        super(Opcodes.ASM9);
    }

    @Override
    public void visitCode() {
        currentBlock = newBlock(0);
    }

    private BasicBlock newBlock(int startOffset) {
        BasicBlock b = new BasicBlock(blockIdCounter++, startOffset);
        blocks.add(b);
        return b;
    }

    @Override
    public void visitLabel(Label label) {
        if (jumpTargets.contains(label) && currentBlock != null) {
            currentBlock = newBlock(offset);
        }
        if (currentBlock != null) {
            blockByLabel.put(label, currentBlock);
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label target) {
        jumpTargets.add(target);
        if (isBranch(opcode)) {
            int branchId = branchIdCounter++;
            currentBlock.branchIds.add(branchId);
            blockByBranchId.put(branchId, currentBlock);
        }
        addEdge(currentBlock, target);
        if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR) {
            currentBlock = newBlock(offset + 3);
        }
        offset += 3;
        super.visitJumpInsn(opcode, target);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        int branchId = branchIdCounter++;
        currentBlock.branchIds.add(branchId);
        blockByBranchId.put(branchId, currentBlock);
        addEdge(currentBlock, dflt);
        for (Label l : labels) addEdge(currentBlock, l);
        currentBlock = newBlock(offset + 10);
        offset += 10;
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        int branchId = branchIdCounter++;
        currentBlock.branchIds.add(branchId);
        blockByBranchId.put(branchId, currentBlock);
        addEdge(currentBlock, dflt);
        for (Label l : labels) addEdge(currentBlock, l);
        currentBlock = newBlock(offset + 10);
        offset += 10;
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RET || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
                || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN
                || opcode == Opcodes.RETURN || opcode == Opcodes.ATHROW) {
            currentBlock = null;
        }
        offset += instructionSize(opcode);
        super.visitInsn(opcode);
    }

    private int instructionSize(int opcode) {
        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) return 1;
        if (opcode >= Opcodes.LCONST_0 && opcode <= Opcodes.DCONST_1) return 1;
        return 1;
    }

    private boolean isBranch(int opcode) {
        return (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE)
                || (opcode >= Opcodes.IF_ICMPEQ && opcode <= Opcodes.IF_ICMPLE)
                || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL
                || opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE;
    }

    private void addEdge(BasicBlock from, Label target) {
        pendingEdges.add(new Object[]{from, target});
    }

    private void resolveEdges() {
        for (Object[] pair : pendingEdges) {
            BasicBlock from = (BasicBlock) pair[0];
            Label target = (Label) pair[1];
            BasicBlock to = blockByLabel.get(target);
            if (to != null) {
                from.successors.add(to);
                to.predecessors.add(from);
            }
        }
    }

    @Override
    public void visitEnd() {
        resolveEdges();
        super.visitEnd();
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public Map<Integer, BasicBlock> getBlockByBranchId() {
        return blockByBranchId;
    }

    public Set<Integer> getAllBranchIds() {
        return new HashSet<>(blockByBranchId.keySet());
    }
}
