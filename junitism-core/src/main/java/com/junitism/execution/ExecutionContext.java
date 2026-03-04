package com.junitism.execution;

import java.lang.reflect.InvocationTargetException;

/**
 * Execution context: classloader for loading target classes.
 */
public class ExecutionContext {

    private static final ThreadLocal<ClassLoader> classLoader = new ThreadLocal<>();

    public static void setClassLoader(ClassLoader loader) {
        classLoader.set(loader);
    }

    public static ClassLoader getClassLoader() {
        ClassLoader cl = classLoader.get();
        return cl != null ? cl : ExecutionContext.class.getClassLoader();
    }

    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, getClassLoader());
    }
}
