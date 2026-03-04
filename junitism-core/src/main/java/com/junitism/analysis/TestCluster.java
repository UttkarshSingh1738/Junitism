package com.junitism.analysis;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central registry: constructors, generators, target methods, dependency resolution.
 */
public class TestCluster {

    private final ClasspathScanner scanner;
    private final Map<TypeInfo, ClassInfo> classInfoByType = new HashMap<>();
    private final ClassHierarchy hierarchy = new ClassHierarchy();
    private final DependencyGraph dependencies = new DependencyGraph();
    private final List<MethodInfo> targetMethods = new ArrayList<>();
    private final Set<TypeInfo> resolvableTypes = new HashSet<>();
    private final Set<TypeInfo> needsMockOrNull = new HashSet<>();
    private String targetClassName;
    private TypeInfo targetType;
    private final Random random = new Random();

    public TestCluster(ClasspathScanner scanner) {
        this.scanner = scanner;
    }

    public void build(String targetClassNameOrPattern) throws IOException {
        Iterable<Path> defaultCp = ClasspathScanner.parseClasspath(System.getProperty("java.class.path", "."));
        scanner.scan(defaultCp);
        buildFromScanned(targetClassNameOrPattern);
    }

    private void buildFromScanned(String targetClassNameOrPattern) throws IOException {
        this.targetClassName = targetClassNameOrPattern;
        Set<String> targetClasses = resolveTargetClasses(targetClassNameOrPattern);
        if (targetClasses.isEmpty()) {
            throw new IllegalArgumentException("No target classes found for: " + targetClassNameOrPattern);
        }

        for (String cn : targetClasses) {
            analyzeAndRegister(cn);
        }

        for (String cn : targetClasses) {
            TypeInfo type = getTypeByInternalName(cn.replace('.', '/'));
            if (type != null) {
                targetType = type;
                ClassInfo info = classInfoByType.get(type);
                if (info != null) {
                    targetMethods.addAll(info.getPublicInstanceMethods());
                }
            }
        }

        resolveDependencies();
    }

    public void build(Iterable<Path> classpath, String targetClassNameOrPattern) throws IOException {
        scanner.scan(classpath);
        buildFromScanned(targetClassNameOrPattern);
    }

    private Set<String> resolveTargetClasses(String pattern) {
        Set<String> result = new HashSet<>();
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            Pattern p = Pattern.compile(regex);
            for (String cn : scanner.getClassNames()) {
                String binary = cn.replace('/', '.');
                if (p.matcher(binary).matches()) {
                    result.add(binary);
                }
            }
        } else {
            String internal = pattern.replace('.', '/');
            if (scanner.hasClass(internal)) {
                result.add(pattern);
            }
        }
        return result;
    }

    private void analyzeAndRegister(String className) {
        String internal = className.replace('.', '/');
        byte[] bytecode = scanner.getBytecode(internal);
        if (bytecode == null) return;

        ClassAnalyzer analyzer = ClassAnalyzer.analyze(bytecode);
        TypeInfo type = analyzer.getTypeInfo();
        if (type == null) return;

        ClassInfo info = new ClassInfo(type, analyzer);
        classInfoByType.put(type, info);

        if (analyzer.getSuperName() != null && !"java/lang/Object".equals(analyzer.getSuperName())) {
            TypeInfo superType = getOrCreateType(analyzer.getSuperName());
            hierarchy.setSuperclass(type, superType);
        }
        for (String iface : analyzer.getInterfaces()) {
            TypeInfo ifaceType = getOrCreateType(iface);
            hierarchy.addInterface(type, ifaceType);
        }

        for (ConstructorInfo ctor : analyzer.getConstructors()) {
            if (ctor.isPublic()) {
                dependencies.addConstructor(ctor);
            }
        }
        for (MethodInfo m : analyzer.getMethods()) {
            if (m.isPublic() && !m.isStatic() && m.getReturnType() != null && !m.getReturnType().isVoid()) {
                dependencies.addGenerator(m);
            }
            if (m.isPublic() && m.isStatic() && m.getReturnType() != null && !m.getReturnType().isVoid()) {
                dependencies.addGenerator(m);
            }
        }
    }

    private TypeInfo getOrCreateType(String internalName) {
        for (TypeInfo t : classInfoByType.keySet()) {
            if (internalName.equals(t.getInternalName())) return t;
        }
        TypeInfo t = new TypeInfo(internalName, "L" + internalName + ";", TypeInfo.Kind.CLASS);
        if (scanner.hasClass(internalName)) {
            byte[] bc = scanner.getBytecode(internalName);
            if (bc != null) {
                ClassAnalyzer a = ClassAnalyzer.analyze(bc);
                t = a.getTypeInfo();
                if (!classInfoByType.containsKey(t)) {
                    classInfoByType.put(t, new ClassInfo(t, a));
                }
            }
        }
        return t;
    }

    private TypeInfo getTypeByInternalName(String internalName) {
        for (TypeInfo t : classInfoByType.keySet()) {
            if (internalName.equals(t.getInternalName())) return t;
        }
        return null;
    }

    private void resolveDependencies() {
        for (TypeInfo type : classInfoByType.keySet()) {
            if (isCommonType(type) || type.isPrimitive()) {
                resolvableTypes.add(type);
            } else if (dependencies.canConstruct(type)) {
                resolvableTypes.add(type);
                for (TypeInfo dep : dependencies.getDependencies(type)) {
                    ensureTypeResolvable(dep);
                }
            } else if (type.isInterface() || (classInfoByType.get(type) != null && classInfoByType.get(type).isAbstract())) {
                List<TypeInfo> concrete = hierarchy.getConcreteSubtypes(type);
                if (!concrete.isEmpty()) {
                    resolvableTypes.add(type);
                    for (TypeInfo c : concrete) {
                        ensureTypeResolvable(c);
                    }
                } else {
                    needsMockOrNull.add(type);
                }
            } else {
                needsMockOrNull.add(type);
            }
        }
    }

    private void ensureTypeResolvable(TypeInfo type) {
        if (resolvableTypes.contains(type)) return;
        if (needsMockOrNull.contains(type)) return;
        if (isCommonType(type) || type.isPrimitive()) {
            resolvableTypes.add(type);
            return;
        }
        if (dependencies.canConstruct(type)) {
            resolvableTypes.add(type);
            for (TypeInfo dep : dependencies.getDependencies(type)) {
                ensureTypeResolvable(dep);
            }
        } else {
            List<TypeInfo> concrete = hierarchy.getConcreteSubtypes(type);
            if (!concrete.isEmpty()) {
                resolvableTypes.add(type);
                for (TypeInfo c : concrete) {
                    ensureTypeResolvable(c);
                }
            } else {
                needsMockOrNull.add(type);
            }
        }
    }

    private boolean isCommonType(TypeInfo type) {
        if (type == null) return false;
        String name = type.getBinaryName();
        return "java.lang.String".equals(name)
                || "java.util.List".equals(name)
                || "java.util.ArrayList".equals(name)
                || "java.util.Set".equals(name)
                || "java.util.HashSet".equals(name)
                || "java.util.Map".equals(name)
                || "java.util.HashMap".equals(name)
                || (name != null && name.startsWith("java.lang.") && type.isPrimitive() == false);
    }

    public List<ConstructorInfo> getConstructors(TypeInfo type) {
        return dependencies.getConstructors(type);
    }

    public List<MethodInfo> getGenerators(TypeInfo type) {
        return dependencies.getGenerators(type);
    }

    public List<MethodInfo> getTargetMethods() {
        return Collections.unmodifiableList(targetMethods);
    }

    public MethodInfo getRandomTargetMethod() {
        if (targetMethods.isEmpty()) return null;
        return targetMethods.get(random.nextInt(targetMethods.size()));
    }

    public TypeInfo getTargetType() {
        return targetType;
    }

    public ClassInfo getClassInfo(TypeInfo type) {
        return classInfoByType.get(type);
    }

    public boolean canConstruct(TypeInfo type) {
        return resolvableTypes.contains(type) || isCommonType(type) || type.isPrimitive();
    }

    public boolean needsMockOrNull(TypeInfo type) {
        return needsMockOrNull.contains(type);
    }

    public ClasspathScanner getScanner() {
        return scanner;
    }

    public ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    public DependencyGraph getDependencies() {
        return dependencies;
    }

    public Set<TypeInfo> getAllTypes() {
        return new HashSet<>(classInfoByType.keySet());
    }

    /**
     * Lightweight class metadata holder.
     */
    public static class ClassInfo {
        private final TypeInfo type;
        private final ClassAnalyzer analyzer;

        ClassInfo(TypeInfo type, ClassAnalyzer analyzer) {
            this.type = type;
            this.analyzer = analyzer;
        }

        public TypeInfo getType() {
            return type;
        }

        public List<MethodInfo> getPublicInstanceMethods() {
            return analyzer.getMethods().stream()
                    .filter(m -> m.isPublic() && !m.isStatic() && !m.isConstructor() && !m.isStaticInitializer())
                    .collect(Collectors.toList());
        }

        public List<ConstructorInfo> getPublicConstructors() {
            return analyzer.getConstructors().stream()
                    .filter(ConstructorInfo::isPublic)
                    .collect(Collectors.toList());
        }

        public List<FieldInfo> getPublicFields() {
            return analyzer.getFields().stream()
                    .filter(FieldInfo::isPublic)
                    .collect(Collectors.toList());
        }

        public boolean isAbstract() {
            return analyzer.isAbstract();
        }

        public boolean isInterface() {
            return analyzer.isInterface();
        }

        public boolean isEnum() {
            return analyzer.isEnum();
        }
    }
}
