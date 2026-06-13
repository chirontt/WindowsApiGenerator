//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link SourceFileSink} that captures the generated source code in memory.
 * <p>
 * The generated content of each file becomes available (keyed by its relative path)
 * once the writer returned by {@link #open(Path)} has been closed. This sink lets
 * tests and tooling inspect generated source without writing to disk.
 * </p>
 */
public class CapturingSink implements SourceFileSink {

    private final Map<Path, StringWriter> files = new LinkedHashMap<>();

    @Override
    public PrintWriter open(Path path) {
        var stringWriter = new StringWriter();
        files.put(path, stringWriter);
        return new PrintWriter(stringWriter);
    }

    /**
     * Gets the generated source code for the specified relative path.
     *
     * @param path the relative path
     * @return the generated source, or {@code null} if no such file was generated
     */
    public String content(Path path) {
        var stringWriter = files.get(path);
        return stringWriter != null ? stringWriter.toString() : null;
    }

    /**
     * Gets the relative paths of all captured source files.
     *
     * @return the set of generated paths
     */
    public Set<Path> generatedPaths() {
        return files.keySet();
    }
}
