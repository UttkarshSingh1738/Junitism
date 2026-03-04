package com.junitism.output;

import com.junitism.analysis.TypeInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages import statements for generated code.
 */
public class ImportResolver {

    private final Set<String> imports = new HashSet<>();

    public void addImport(String qualifiedName) {
        if (qualifiedName != null && !qualifiedName.startsWith("java.lang.")) {
            imports.add(qualifiedName);
        }
    }

    public void addImport(TypeInfo type) {
        if (type != null && type.getBinaryName() != null) {
            addImport(type.getBinaryName());
        }
    }

    public Set<String> getImports() {
        return new HashSet<>(imports);
    }

    public String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
