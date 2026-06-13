//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * Destination for generated Java source files.
 * <p>
 * This is the seam between code generation (deciding what source to emit) and
 * persistence (deciding where the bytes go). Implementations decide whether the
 * generated code is written to disk ({@link FileSink}), discarded ({@link NullSink}),
 * or captured in memory ({@link CapturingSink}).
 * </p>
 */
public interface SourceFileSink {

    /**
     * Opens a writer for the source file at the specified relative path.
     * <p>
     * The caller is responsible for closing the returned writer.
     * </p>
     *
     * @param path the file path, relative to the output location
     * @return a print writer for the file content
     */
    PrintWriter open(Path path);
}
