//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 * {@link SourceFileSink} that discards all generated source code.
 * <p>
 * Used for dry runs that exercise the full generation without writing any files.
 * </p>
 */
public class NullSink implements SourceFileSink {

    @Override
    public PrintWriter open(Path path) {
        return new PrintWriter(OutputStream.nullOutputStream());
    }
}
