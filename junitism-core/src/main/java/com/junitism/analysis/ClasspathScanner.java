package com.junitism.analysis;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Loads classes from JARs, directories, or individual .class files.
 * Builds a Map of className (internal form) to bytecode.
 */
public class ClasspathScanner {

    private final Map<String, byte[]> bytecodeByClass = new HashMap<>();

    public void scan(Iterable<Path> classpathEntries) throws IOException {
        bytecodeByClass.clear();
        for (Path entry : classpathEntries) {
            if (Files.notExists(entry)) continue;
            if (Files.isDirectory(entry)) {
                scanDirectory(entry);
            } else if (entry.toString().toLowerCase().endsWith(".jar")
                    || entry.toString().toLowerCase().endsWith(".zip")) {
                scanJar(entry);
            } else if (entry.toString().endsWith(".class")) {
                scanClassFile(entry);
            }
        }
    }

    private void scanDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    scanClassFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanJar(Path jarPath) throws IOException {
        try (InputStream is = Files.newInputStream(jarPath);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class") && !entry.isDirectory()) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    byte[] bytes = zis.readAllBytes();
                    bytecodeByClass.put(className.replace('.', '/'), bytes);
                }
                zis.closeEntry();
            }
        }
    }

    private void scanClassFile(Path classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        String className = getClassNameFromBytecode(bytes);
        if (className != null) {
            bytecodeByClass.put(className, bytes);
        }
    }

    private String getClassNameFromBytecode(byte[] bytes) {
        try {
            ClassReader cr = new ClassReader(bytes);
            return cr.getClassName();
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] getBytecode(String internalClassName) {
        return bytecodeByClass.get(internalClassName);
    }

    public Map<String, byte[]> getAllBytecode() {
        return new HashMap<>(bytecodeByClass);
    }

    public boolean hasClass(String internalClassName) {
        return bytecodeByClass.containsKey(internalClassName);
    }

    public Iterable<String> getClassNames() {
        return bytecodeByClass.keySet();
    }

    /**
     * Parse classpath string (colon or semicolon separated) into paths.
     */
    public static Iterable<Path> parseClasspath(String classpath) {
        String separator = System.getProperty("path.separator", ":");
        return java.util.Arrays.stream(classpath.split(java.util.regex.Pattern.quote(separator)))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Path::of)
                .toList();
    }
}
