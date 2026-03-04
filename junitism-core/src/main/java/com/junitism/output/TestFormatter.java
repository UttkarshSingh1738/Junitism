package com.junitism.output;

import com.junitism.analysis.TypeInfo;

/**
 * Variable naming and formatting for generated tests.
 */
public class TestFormatter {

    public String generateVariableName(TypeInfo type, int index) {
        if (type == null) return "var" + index;
        String base = decapitalize(type.getSimpleName());
        return base + index;
    }

    private String decapitalize(String name) {
        if (name == null || name.length() < 2) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public String deriveTestPackage(String targetClassName) {
        int lastDot = targetClassName.lastIndexOf('.');
        return lastDot > 0 ? targetClassName.substring(0, lastDot) : "";
    }

    public String simpleTestClassName(String targetClassName) {
        int lastDot = targetClassName.lastIndexOf('.');
        String simple = lastDot >= 0 ? targetClassName.substring(lastDot + 1) : targetClassName;
        return simple + "Test";
    }
}
