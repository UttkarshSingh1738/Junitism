package com.junitism.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for a constructor.
 */
public final class ConstructorInfo {

    private final TypeInfo declaringClass;
    private final String descriptor;
    private final List<TypeInfo> parameterTypes;
    private final int accessFlags;

    public ConstructorInfo(TypeInfo declaringClass, String descriptor,
                           List<TypeInfo> parameterTypes, int accessFlags) {
        this.declaringClass = declaringClass;
        this.descriptor = descriptor;
        this.parameterTypes = parameterTypes != null ? new ArrayList<>(parameterTypes) : new ArrayList<>();
        this.accessFlags = accessFlags;
    }

    public TypeInfo getDeclaringClass() {
        return declaringClass;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public List<TypeInfo> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public boolean isPublic() {
        return (accessFlags & 0x0001) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstructorInfo that = (ConstructorInfo) o;
        return Objects.equals(declaringClass, that.declaringClass)
                && Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass, descriptor);
    }
}
