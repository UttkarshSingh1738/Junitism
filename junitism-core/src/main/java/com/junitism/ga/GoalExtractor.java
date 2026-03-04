package com.junitism.ga;

import com.junitism.analysis.ClasspathScanner;
import com.junitism.instrumentation.ControlDependencyGraph;
import com.junitism.instrumentation.ControlFlowGraphBuilder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts branch coverage goals from target class bytecode.
 */
public class GoalExtractor {

    public static Set<BranchCoverageGoal> extractGoals(String targetClassName, ClasspathScanner scanner) {
        String internal = targetClassName.replace('.', '/');
        byte[] bytecode = scanner.getBytecode(internal);
        if (bytecode == null) return Set.of();

        Set<BranchCoverageGoal> goals = new HashSet<>();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if ("<clinit>".equals(name)) return super.visitMethod(access, name, descriptor, signature, exceptions);
                ControlFlowGraphBuilder cfg = new ControlFlowGraphBuilder(internal, name);
                return new MethodVisitor(Opcodes.ASM9, cfg) {
                    @Override
                    public void visitEnd() {
                        for (Integer bid : cfg.getAllBranchIds()) {
                            goals.add(new BranchCoverageGoal(bid, true));
                            goals.add(new BranchCoverageGoal(bid, false));
                        }
                        super.visitEnd();
                    }
                };
            }
        }, ClassReader.EXPAND_FRAMES);

        return goals;
    }

    public static ControlDependencyGraph buildCDG(String targetClassName, ClasspathScanner scanner) {
        String internal = targetClassName.replace('.', '/');
        byte[] bytecode = scanner.getBytecode(internal);
        if (bytecode == null) return new ControlDependencyGraph(List.of());

        List<ControlFlowGraphBuilder.BasicBlock> allBlocks = new ArrayList<>();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if ("<clinit>".equals(name)) return super.visitMethod(access, name, descriptor, signature, exceptions);
                ControlFlowGraphBuilder cfg = new ControlFlowGraphBuilder(internal, name);
                return new MethodVisitor(Opcodes.ASM9, cfg) {
                    @Override
                    public void visitEnd() {
                        allBlocks.addAll(cfg.getBlocks());
                        super.visitEnd();
                    }
                };
            }
        }, ClassReader.EXPAND_FRAMES);

        return new ControlDependencyGraph(allBlocks);
    }
}
