package com.junitism.analysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * ASM-based bytecode analyzer. Extracts methods, constructors, fields, superclass, interfaces.
 */
public class ClassAnalyzer extends ClassVisitor {

    private String className;
    private String superName;
    private String[] interfaces;
    private int classAccess;
    private TypeInfo typeInfo;
    private final List<MethodInfo> methods = new ArrayList<>();
    private final List<ConstructorInfo> constructors = new ArrayList<>();
    private final List<FieldInfo> fields = new ArrayList<>();

    public ClassAnalyzer() {
        super(Opcodes.ASM9);
    }

    public ClassAnalyzer(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                     String superName, String[] interfaces) {
        this.className = name;
        this.superName = superName;
        this.interfaces = interfaces != null ? interfaces : new String[0];
        this.classAccess = access;
        this.typeInfo = createTypeInfo(name, access);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private TypeInfo createTypeInfo(String internalName, int access) {
        TypeInfo.Kind kind = TypeInfo.Kind.CLASS;
        if ((access & Opcodes.ACC_INTERFACE) != 0) kind = TypeInfo.Kind.INTERFACE;
        else if ((access & Opcodes.ACC_ENUM) != 0) kind = TypeInfo.Kind.ENUM;
        else if (isRecord(access)) kind = TypeInfo.Kind.RECORD;
        return new TypeInfo(internalName, "L" + internalName + ";", kind);
    }

    private boolean isRecord(int access) {
        return (access & 0x10000) != 0; // ACC_RECORD
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        if ("<clinit>".equals(name)) return super.visitMethod(access, name, descriptor, signature, exceptions);

        Type methodType = Type.getMethodType(descriptor);
        TypeInfo returnType = toTypeInfo(methodType.getReturnType());
        List<TypeInfo> paramTypes = new ArrayList<>();
        for (Type argType : methodType.getArgumentTypes()) {
            paramTypes.add(toTypeInfo(argType));
        }

        List<String> excTypes = exceptions != null ? List.of(exceptions) : List.of();

        if ("<init>".equals(name)) {
            constructors.add(new ConstructorInfo(typeInfo, descriptor, paramTypes, access));
        } else {
            methods.add(new MethodInfo(name, descriptor, returnType, paramTypes, access, typeInfo, excTypes));
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
                                   String signature, Object value) {
        TypeInfo fieldType = toTypeInfo(Type.getType(descriptor));
        fields.add(new FieldInfo(name, descriptor, fieldType, access, typeInfo));
        return super.visitField(access, name, descriptor, signature, value);
    }

    private static TypeInfo toTypeInfo(Type t) {
        if (t == null) return null;
        String desc = t.getDescriptor();
        if (t.getSort() == Type.VOID) return TypeInfo.fromDescriptor(desc);
        if (t.getSort() == Type.ARRAY) return TypeInfo.fromDescriptor(desc);
        if (t.getSort() == Type.OBJECT) {
            String internal = t.getInternalName();
            return new TypeInfo(internal, desc, TypeInfo.Kind.CLASS);
        }
        return TypeInfo.fromDescriptor(desc);
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public String getClassName() {
        return className;
    }

    public String getSuperName() {
        return superName;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public int getClassAccess() {
        return classAccess;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public List<ConstructorInfo> getConstructors() {
        return constructors;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public boolean isInterface() {
        return (classAccess & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isEnum() {
        return (classAccess & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isAbstract() {
        return (classAccess & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Analyze bytecode and return the analyzer with populated data.
     */
    public static ClassAnalyzer analyze(byte[] bytecode) {
        ClassAnalyzer analyzer = new ClassAnalyzer();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(analyzer, ClassReader.EXPAND_FRAMES);
        return analyzer;
    }
}
