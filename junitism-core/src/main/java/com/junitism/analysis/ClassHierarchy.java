package com.junitism.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inheritance and interface tree for type resolution.
 */
public class ClassHierarchy {

    private final Map<TypeInfo, TypeInfo> superclass = new HashMap<>();
    private final Map<TypeInfo, List<TypeInfo>> interfaces = new HashMap<>();
    private final Map<TypeInfo, Set<TypeInfo>> subtypes = new HashMap<>();
    private final Map<TypeInfo, Set<TypeInfo>> implementors = new HashMap<>();

    public void setSuperclass(TypeInfo type, TypeInfo superType) {
        if (type != null && superType != null && !"java/lang/Object".equals(superType.getInternalName())) {
            superclass.put(type, superType);
            subtypes.computeIfAbsent(superType, k -> new HashSet<>()).add(type);
        }
    }

    public void addInterface(TypeInfo type, TypeInfo iface) {
        if (type != null && iface != null) {
            interfaces.computeIfAbsent(type, k -> new ArrayList<>()).add(iface);
            implementors.computeIfAbsent(iface, k -> new HashSet<>()).add(type);
        }
    }

    public TypeInfo getSuperclass(TypeInfo type) {
        return superclass.get(type);
    }

    public List<TypeInfo> getInterfaces(TypeInfo type) {
        return interfaces.getOrDefault(type, Collections.emptyList());
    }

    public Set<TypeInfo> getSubtypes(TypeInfo type) {
        return subtypes.getOrDefault(type, Collections.emptySet());
    }

    public Set<TypeInfo> getImplementors(TypeInfo type) {
        return implementors.getOrDefault(type, Collections.emptySet());
    }

    public boolean isSubtypeOf(TypeInfo subtype, TypeInfo supertype) {
        if (subtype == null || supertype == null) return false;
        if (subtype.equals(supertype)) return true;
        TypeInfo current = superclass.get(subtype);
        while (current != null) {
            if (current.equals(supertype)) return true;
            current = superclass.get(current);
        }
        for (TypeInfo iface : getInterfaces(subtype)) {
            if (iface.equals(supertype) || isSubtypeOf(iface, supertype)) return true;
        }
        return false;
    }

    public List<TypeInfo> getConcreteSubtypes(TypeInfo type) {
        Set<TypeInfo> result = new HashSet<>();
        collectConcreteSubtypes(type, result);
        return new ArrayList<>(result);
    }

    private void collectConcreteSubtypes(TypeInfo type, Set<TypeInfo> result) {
        for (TypeInfo sub : getSubtypes(type)) {
            if (sub.isConcrete()) result.add(sub);
            collectConcreteSubtypes(sub, result);
        }
        for (TypeInfo impl : getImplementors(type)) {
            if (impl.isConcrete()) result.add(impl);
            collectConcreteSubtypes(impl, result);
        }
    }
}
