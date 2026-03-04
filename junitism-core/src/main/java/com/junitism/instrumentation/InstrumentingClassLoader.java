package com.junitism.instrumentation;

import com.junitism.analysis.ClasspathScanner;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassLoader that instruments target classes with branch distance probes.
 */
public class InstrumentingClassLoader extends ClassLoader {

    private final ClasspathScanner scanner;
    private final Set<String> targetPackages = new HashSet<>();
    private final Set<String> targetClasses = new HashSet<>();

    public InstrumentingClassLoader(ClassLoader parent, ClasspathScanner scanner) {
        super(parent);
        this.scanner = scanner;
    }

    public InstrumentingClassLoader(ClasspathScanner scanner) {
        super(InstrumentingClassLoader.class.getClassLoader());
        this.scanner = scanner;
    }

    public void addTargetPackage(String packageName) {
        targetPackages.add(packageName.replace('.', '/'));
    }

    public void addTargetClass(String className) {
        targetClasses.add(className.replace('.', '/'));
    }

    public void setTargetPackages(Iterable<String> packages) {
        targetPackages.clear();
        for (String p : packages) {
            targetPackages.add(p.replace('.', '/'));
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        byte[] original = scanner.getBytecode(internalName);
        if (original == null) {
            return super.findClass(name);
        }
        byte[] bytecode = shouldInstrument(internalName) ? instrument(original) : original;
        return defineClass(name, bytecode, 0, bytecode.length);
    }

    private boolean shouldInstrument(String internalName) {
        if (targetClasses.contains(internalName)) return true;
        for (String pkg : targetPackages) {
            if (internalName.startsWith(pkg)) return true;
        }
        return targetPackages.isEmpty() && targetClasses.isEmpty();
    }

    private byte[] instrument(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    Class<?> c1 = loadClass(type1.replace('/', '.'));
                    Class<?> c2 = loadClass(type2.replace('/', '.'));
                    Class<?> c = c1;
                    while (!c.isAssignableFrom(c2)) {
                        c = c.getSuperclass();
                        if (c == null) return "java/lang/Object";
                    }
                    return c.getName().replace('.', '/');
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        };
        ClassVisitor cv = new BranchInstrumentingClassVisitor(Opcodes.ASM9, cw);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    public Class<?> loadInstrumentedClass(String className) throws ClassNotFoundException {
        return loadClass(className);
    }
}
