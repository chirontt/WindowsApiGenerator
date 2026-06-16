//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.graalvm;

import net.codecrete.windowsapi.graalvm.json.Downcall;
import net.codecrete.windowsapi.graalvm.json.ReachabilityMetadata;
import net.codecrete.windowsapi.graalvm.json.Upcall;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes {@link ReachabilityMetadata} as a pretty-printed JSON file.
 * <p>
 * The output is hand-rolled to keep the generator free of external dependencies.
 * Only the {@code foreign} section is emitted. The {@code linkerOptions} object
 * of a downcall is only written if {@code captureCallState} is {@code true}.
 * </p>
 */
public final class ReachabilityMetadataWriter {

    /**
     * Writes the reachability metadata to the given file.
     *
     * @param metadata the reachability metadata
     * @param file     the absolute path of the file to write
     */
    public void writeToFile(ReachabilityMetadata metadata, Path file) {
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(metadata, writer);
        } catch (IOException exc) {
            throw new UncheckedIOException("Unable to write reachability metadata to " + file, exc);
        }
    }

    /**
     * Writes the reachability metadata to the given writer.
     *
     * @param metadata the reachability metadata
     * @param writer   the writer
     * @throws IOException if writing fails
     */
    void write(ReachabilityMetadata metadata, Writer writer) throws IOException {
        var foreign = metadata.foreign();
        writer.write("{\n");
        writer.write("  \"foreign\": {\n");
        writeDowncalls(writer, foreign.downcalls());
        writer.write(",\n");
        writeUpcalls(writer, foreign.upcalls());
        writer.write("\n");
        writer.write("  }\n");
        writer.write("}\n");
    }

    private void writeDowncalls(Writer writer, List<Downcall> downcalls) throws IOException {
        writer.write("    \"downcalls\": ");
        if (downcalls.isEmpty()) {
            writer.write("[]");
            return;
        }

        writer.write("[\n");
        for (var i = 0; i < downcalls.size(); i += 1) {
            var downcall = downcalls.get(i);
            writer.write("      {\n");
            writer.write("        \"returnType\": ");
            writeString(writer, downcall.returnType());
            writer.write(",\n");
            writer.write("        \"parameterTypes\": ");
            writeStringArray(writer, downcall.parameterTypes());
            if (downcall.linkerOptions().captureCallState()) {
                writer.write(",\n");
                writer.write("        \"linkerOptions\": {\n");
                writer.write("          \"captureCallState\": true\n");
                writer.write("        }\n");
            } else {
                writer.write("\n");
            }
            writer.write(i < downcalls.size() - 1 ? "      },\n" : "      }\n");
        }
        writer.write("    ]");
    }

    private void writeUpcalls(Writer writer, List<Upcall> upcalls) throws IOException {
        writer.write("    \"upcalls\": ");
        if (upcalls.isEmpty()) {
            writer.write("[]");
            return;
        }

        writer.write("[\n");
        for (var i = 0; i < upcalls.size(); i += 1) {
            var upcall = upcalls.get(i);
            writer.write("      {\n");
            writer.write("        \"returnType\": ");
            writeString(writer, upcall.returnType());
            writer.write(",\n");
            writer.write("        \"parameterTypes\": ");
            writeStringArray(writer, upcall.parameterTypes());
            writer.write("\n");
            writer.write(i < upcalls.size() - 1 ? "      },\n" : "      }\n");
        }
        writer.write("    ]");
    }

    private void writeStringArray(Writer writer, List<String> values) throws IOException {
        writer.write('[');
        for (var i = 0; i < values.size(); i += 1) {
            if (i > 0)
                writer.write(", ");
            writeString(writer, values.get(i));
        }
        writer.write(']');
    }

    private void writeString(Writer writer, String value) throws IOException {
        writer.write('"');
        for (var i = 0; i < value.length(); i += 1) {
            var c = value.charAt(i);
            switch (c) {
                case '"' -> writer.write("\\\"");
                case '\\' -> writer.write("\\\\");
                case '\n' -> writer.write("\\n");
                case '\r' -> writer.write("\\r");
                case '\t' -> writer.write("\\t");
                default -> {
                    if (c < 0x20)
                        writer.write(String.format("\\u%04x", (int) c));
                    else
                        writer.write(c);
                }
            }
        }
        writer.write('"');
    }
}
