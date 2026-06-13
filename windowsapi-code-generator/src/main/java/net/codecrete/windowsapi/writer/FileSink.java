//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link SourceFileSink} that writes the generated source files below an output directory.
 * <p>
 * It records the relative paths of all files it has created. They are available
 * through {@link #generatedPaths()} and can be used, for example, to delete stale files.
 * </p>
 */
public class FileSink implements SourceFileSink {

    private final Path outputDirectory;
    private final Set<Path> generatedPaths = new HashSet<>();

    /**
     * Creates a new instance writing below the specified output directory.
     *
     * @param outputDirectory the output directory (must exist)
     */
    public FileSink(Path outputDirectory) {
        if (Files.notExists(outputDirectory))
            throw new IllegalArgumentException("Output directory does not exist: " + outputDirectory);
        this.outputDirectory = outputDirectory;
    }

    @Override
    public PrintWriter open(Path path) {
        var fullPath = outputDirectory.resolve(path);
        generatedPaths.add(path);

        try {
            // create the directory if needed
            var directory = fullPath.getParent().toFile();
            if (!directory.exists()) {
                var success = directory.mkdirs();
                if (!success)
                    throw new GenerationException("Unable to create directory " + directory);
            }

            // create the file
            var file = fullPath.toFile();
            return new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8));

        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write Java file " + path, exception);
        }
    }

    /**
     * Gets the relative paths of all source files created by this sink.
     *
     * @return the set of generated paths, relative to the output directory
     */
    public Set<Path> generatedPaths() {
        return generatedPaths;
    }
}
