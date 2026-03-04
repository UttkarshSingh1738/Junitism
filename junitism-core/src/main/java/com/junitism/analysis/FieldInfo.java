package com.junitism.analysis;

import java.util.Objects;

/**
 * Metadata for a discovered field.
 */
public final class FieldInfo {

    private final String name;
    private final String descriptor;
    private final TypeInfo type;
    private final int accessFlags;
    private final TypeInfo declaringClass;

    public FieldInfo(String name, String descriptor, TypeInfo type, int accessFlags, TypeInfo declaringClass) {
        this.name = name;
        this.descriptor = descriptor;
        this.type = type;
        this.accessFlags = accessFlags;
        this.declaringClass = declaringClass;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public TypeInfo getType() {
        return type;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public TypeInfo getDeclaringClass() {
        return declaringClass;
    }

    public boolean isPublic() {
        return (accessFlags & 0x0001) != 0;
    }

    public boolean isStatic() {
        return (accessFlags & 0x0008) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo that = (FieldInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(declaringClass, that.declaringClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, declaringClass);
    }
}
