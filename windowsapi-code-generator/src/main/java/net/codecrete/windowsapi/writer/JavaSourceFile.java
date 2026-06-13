//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.Namespace;
import net.codecrete.windowsapi.metadata.Type;

import java.io.PrintWriter;

/**
 * Immutable state of the Java source file currently being written.
 * <p>
 * It is the single source of truth for the "current file": created when a file
 * is opened and passed to the writing functions that need it. This avoids the
 * scattered, independently managed fields that previously had to be reset one by one.
 * </p>
 *
 * @param writer      the print writer for the file
 * @param namespace   the namespace of the file
 * @param type        the metadata type that is the basis for the file (or {@code null})
 * @param packageName the Java package name of the file
 * @param className   the Java class name of the file
 * @param <T>         the metadata type
 */
record JavaSourceFile<T extends Type>(
        PrintWriter writer,
        Namespace namespace,
        T type,
        String packageName,
        String className
) { }
