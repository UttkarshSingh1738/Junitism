package com.junitism.analysis;

import java.util.Objects;

/**
 * Metadata for a discovered type (class, interface, enum, record, array, primitive).
 */
public final class TypeInfo {

    public enum Kind {
        CLASS,
        INTERFACE,
        ENUM,
        RECORD,
        ARRAY,
        PRIMITIVE,
        VOID
    }

    private final String internalName;
    private final String descriptor;
    private final Kind kind;
    private final TypeInfo componentType; // for arrays
    private final String simpleName;

    public TypeInfo(String internalName, String descriptor, Kind kind) {
        this(internalName, descriptor, kind, null);
    }

    public TypeInfo(String internalName, String descriptor, Kind kind, TypeInfo componentType) {
        this.internalName = internalName;
        this.descriptor = descriptor;
        this.kind = kind;
        this.componentType = componentType;
        this.simpleName = deriveSimpleName(internalName);
    }

    private static String deriveSimpleName(String internalName) {
        if (internalName == null || internalName.isEmpty()) return "";
        int lastSlash = internalName.lastIndexOf('/');
        String name = lastSlash >= 0 ? internalName.substring(lastSlash + 1) : internalName;
        int lastDollar = name.lastIndexOf('$');
        return lastDollar >= 0 ? name.substring(lastDollar + 1) : name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Kind getKind() {
        return kind;
    }

    public TypeInfo getComponentType() {
        return componentType;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getBinaryName() {
        return internalName != null ? internalName.replace('/', '.') : null;
    }

    public boolean isPrimitive() {
        return kind == Kind.PRIMITIVE;
    }

    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    public boolean isInterface() {
        return kind == Kind.INTERFACE;
    }

    public boolean isEnum() {
        return kind == Kind.ENUM;
    }

    public boolean isAbstract() {
        return kind == Kind.INTERFACE;
    }

    public boolean isConcrete() {
        return kind == Kind.CLASS || kind == Kind.ENUM || kind == Kind.RECORD;
    }

    public boolean isVoid() {
        return kind == Kind.VOID;
    }

    public static TypeInfo fromDescriptor(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) return null;
        char first = descriptor.charAt(0);
        if (first == 'V') {
            return new TypeInfo("void", descriptor, Kind.VOID);
        }
        if (first == '[') {
            TypeInfo component = fromDescriptor(descriptor.substring(1));
            String internalName = descriptor.replace('/', '.');
            return new TypeInfo(internalName, descriptor, Kind.ARRAY, component);
        }
        if (first == 'L') {
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            return new TypeInfo(internalName, descriptor, Kind.CLASS);
        }
        if ("ZBCSIJFD".indexOf(first) >= 0) {
            String name = primitiveName(first);
            return new TypeInfo(name, descriptor, Kind.PRIMITIVE);
        }
        return null;
    }

    private static String primitiveName(char c) {
        return switch (c) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            default -> "unknown";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) o;
        return Objects.equals(descriptor, typeInfo.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor);
    }

    @Override
    public String toString() {
        return getBinaryName() != null ? getBinaryName() : descriptor;
    }
}
