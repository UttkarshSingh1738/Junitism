package com.junitism.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What types are needed to construct a given type.
 */
public class DependencyGraph {

    private final Map<TypeInfo, List<TypeInfo>> dependencies = new HashMap<>();
    private final Map<TypeInfo, List<ConstructorInfo>> constructors = new HashMap<>();
    private final Map<TypeInfo, List<MethodInfo>> generators = new HashMap<>();

    public void addConstructor(ConstructorInfo ctor) {
        TypeInfo type = ctor.getDeclaringClass();
        constructors.computeIfAbsent(type, k -> new ArrayList<>()).add(ctor);
        dependencies.computeIfAbsent(type, k -> new ArrayList<>()).addAll(ctor.getParameterTypes());
    }

    public void addGenerator(MethodInfo method) {
        TypeInfo returnType = method.getReturnType();
        if (returnType != null && !returnType.isVoid()) {
            generators.computeIfAbsent(returnType, k -> new ArrayList<>()).add(method);
        }
    }

    public List<TypeInfo> getDependencies(TypeInfo type) {
        return dependencies.getOrDefault(type, Collections.emptyList());
    }

    public List<ConstructorInfo> getConstructors(TypeInfo type) {
        return constructors.getOrDefault(type, Collections.emptyList());
    }

    public List<MethodInfo> getGenerators(TypeInfo type) {
        return generators.getOrDefault(type, Collections.emptyList());
    }

    public boolean canConstruct(TypeInfo type) {
        return !getConstructors(type).isEmpty() || !getGenerators(type).isEmpty();
    }

    /**
     * Get all types that must be constructed before the given type (transitive).
     */
    public Set<TypeInfo> getTransitiveDependencies(TypeInfo type) {
        Set<TypeInfo> result = new HashSet<>();
        collectDependencies(type, result);
        return result;
    }

    private void collectDependencies(TypeInfo type, Set<TypeInfo> result) {
        for (TypeInfo dep : getDependencies(type)) {
            if (result.add(dep)) {
                collectDependencies(dep, result);
            }
        }
    }
}
