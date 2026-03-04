package com.junitism.testsubjects;

/**
 * Simple string utilities for integration tests.
 */
public class StringUtils {

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String reverse(String s) {
        if (s == null) return null;
        return new StringBuilder(s).reverse().toString();
    }
}
