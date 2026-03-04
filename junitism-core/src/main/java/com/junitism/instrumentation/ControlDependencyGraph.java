package com.junitism.instrumentation;

import com.junitism.instrumentation.ControlFlowGraphBuilder.BasicBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Control Dependency Graph: which branches are control-dependent on which.
 * Built from CFG via post-dominator tree.
 */
public class ControlDependencyGraph {

    private final Map<Integer, Set<Integer>> dependents = new HashMap<>();
    private final Map<Integer, Set<Integer>> dependencies = new HashMap<>();
    private final Set<Integer> rootBranches = new HashSet<>();
    private final List<BasicBlock> blocks;

    public ControlDependencyGraph(List<BasicBlock> blocks) {
        this.blocks = blocks;
        build();
    }

    private void build() {
        if (blocks.isEmpty()) return;

        BasicBlock entry = blocks.get(0);
        Map<BasicBlock, BasicBlock> postDominators = computePostDominators(entry);

        for (BasicBlock block : blocks) {
            for (int branchId : block.branchIds) {
                if (block.successors.size() >= 2) {
                    for (BasicBlock succ : block.successors) {
                        Set<BasicBlock> controlDep = getControlDependentNodes(succ, block, postDominators);
                        for (BasicBlock dep : controlDep) {
                            for (int depBranchId : dep.branchIds) {
                                dependents.computeIfAbsent(branchId, k -> new HashSet<>()).add(depBranchId);
                                dependencies.computeIfAbsent(depBranchId, k -> new HashSet<>()).add(branchId);
                            }
                        }
                    }
                }
            }
        }

        Set<Integer> allWithDeps = new HashSet<>();
        for (Set<Integer> deps : dependencies.values()) {
            allWithDeps.addAll(deps);
        }
        for (BasicBlock b : blocks) {
            for (int branchId : b.branchIds) {
                if (!allWithDeps.contains(branchId)) {
                    rootBranches.add(branchId);
                }
            }
        }
        if (rootBranches.isEmpty() && !blocks.isEmpty()) {
            blocks.forEach(b -> b.branchIds.forEach(rootBranches::add));
        }
    }

    private Map<BasicBlock, BasicBlock> computePostDominators(BasicBlock entry) {
        Map<BasicBlock, BasicBlock> pd = new HashMap<>();
        List<BasicBlock> reverseOrder = reversePostOrder(entry);
        for (BasicBlock b : blocks) {
            pd.put(b, null);
        }
        pd.put(entry, entry);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock b : reverseOrder) {
                if (b == entry) continue;
                List<BasicBlock> succs = new ArrayList<>(b.successors);
                if (succs.isEmpty()) continue;
                BasicBlock newPd = succs.get(0);
                for (int i = 1; i < succs.size(); i++) {
                    newPd = commonPostDominator(newPd, succs.get(i), pd);
                }
                if (pd.get(b) != newPd) {
                    pd.put(b, newPd);
                    changed = true;
                }
            }
        }
        return pd;
    }

    private List<BasicBlock> reversePostOrder(BasicBlock entry) {
        List<BasicBlock> result = new ArrayList<>();
        Set<BasicBlock> visited = new HashSet<>();
        postOrder(entry, visited, result);
        return result;
    }

    private void postOrder(BasicBlock b, Set<BasicBlock> visited, List<BasicBlock> result) {
        if (!visited.add(b)) return;
        for (BasicBlock succ : b.successors) {
            postOrder(succ, visited, result);
        }
        result.add(b);
    }

    private BasicBlock commonPostDominator(BasicBlock a, BasicBlock b, Map<BasicBlock, BasicBlock> pd) {
        Set<BasicBlock> path = new HashSet<>();
        BasicBlock x = a;
        while (x != null) {
            path.add(x);
            x = pd.get(x);
        }
        x = b;
        while (x != null) {
            if (path.contains(x)) return x;
            x = pd.get(x);
        }
        return null;
    }

    private Set<BasicBlock> getControlDependentNodes(BasicBlock node, BasicBlock branchBlock,
                                                     Map<BasicBlock, BasicBlock> postDominators) {
        Set<BasicBlock> result = new HashSet<>();
        BasicBlock runner = node;
        BasicBlock branchPd = postDominators.get(branchBlock);
        while (runner != null && runner != branchPd) {
            result.add(runner);
            runner = postDominators.get(runner);
        }
        return result;
    }

    public Set<Integer> getStructuralChildren(int branchId) {
        return dependents.getOrDefault(branchId, Set.of());
    }

    public Set<Integer> getRootBranches() {
        return new HashSet<>(rootBranches);
    }

    public Set<Integer> getDependencies(int branchId) {
        return dependencies.getOrDefault(branchId, Set.of());
    }
}
