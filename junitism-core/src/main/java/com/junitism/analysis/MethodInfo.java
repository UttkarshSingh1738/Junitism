package com.junitism.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for a discovered method.
 */
public final class MethodInfo {

    private final String name;
    private final String descriptor;
    private final TypeInfo returnType;
    private final List<TypeInfo> parameterTypes;
    private final int accessFlags;
    private final TypeInfo declaringClass;
    private final List<String> exceptionTypes;

    public MethodInfo(String name, String descriptor, TypeInfo returnType,
                      List<TypeInfo> parameterTypes, int accessFlags, TypeInfo declaringClass) {
        this(name, descriptor, returnType, parameterTypes, accessFlags, declaringClass, List.of());
    }

    public MethodInfo(String name, String descriptor, TypeInfo returnType,
                      List<TypeInfo> parameterTypes, int accessFlags, TypeInfo declaringClass,
                      List<String> exceptionTypes) {
        this.name = name;
        this.descriptor = descriptor;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes != null ? new ArrayList<>(parameterTypes) : new ArrayList<>();
        this.accessFlags = accessFlags;
        this.declaringClass = declaringClass;
        this.exceptionTypes = exceptionTypes != null ? new ArrayList<>(exceptionTypes) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public TypeInfo getReturnType() {
        return returnType;
    }

    public List<TypeInfo> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public TypeInfo getDeclaringClass() {
        return declaringClass;
    }

    public List<String> getExceptionTypes() {
        return Collections.unmodifiableList(exceptionTypes);
    }

    public boolean isPublic() {
        return (accessFlags & 0x0001) != 0;
    }

    public boolean isStatic() {
        return (accessFlags & 0x0008) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & 0x0400) != 0;
    }

    public boolean isConstructor() {
        return "<init>".equals(name);
    }

    public boolean isStaticInitializer() {
        return "<clinit>".equals(name);
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(declaringClass != null ? declaringClass.getBinaryName() : "?");
        sb.append(".").append(name).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i).getBinaryName());
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(" : ").append(returnType.getBinaryName());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(declaringClass, that.declaringClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, descriptor, declaringClass);
    }
}
