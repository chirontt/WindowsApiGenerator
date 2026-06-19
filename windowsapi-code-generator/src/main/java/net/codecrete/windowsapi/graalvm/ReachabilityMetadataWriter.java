//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.graalvm;

import net.codecrete.windowsapi.events.Event;
import net.codecrete.windowsapi.events.EventListener;
import net.codecrete.windowsapi.graalvm.json.Downcall;
import net.codecrete.windowsapi.graalvm.json.JsonWriter;
import net.codecrete.windowsapi.graalvm.json.Method;
import net.codecrete.windowsapi.graalvm.json.ReachabilityMetadata;
import net.codecrete.windowsapi.graalvm.json.ReflectionObject;
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
 * The JSON is produced with the {@code org.cthing:jsonwriter} library. The
 * {@code foreign} section is always emitted; the {@code reflection} section is
 * only emitted if it contains at least one entry. The {@code linkerOptions} object
 * of a downcall is only written if {@code captureCallState} is {@code true}.
 * </p>
 */
@SuppressWarnings("java:S1192")
public final class ReachabilityMetadataWriter {

    private final EventListener eventListener;

    /**
     * Creates a new instance.
     *
     * @param eventListener event listener to notify (about generated files)
     */
    public ReachabilityMetadataWriter(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Writes the reachability metadata to the given file.
     *
     * @param metadata the reachability metadata
     * @param file     the absolute path of the file to write
     */
    public void writeToFile(ReachabilityMetadata metadata, Path file) {
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(metadata, writer);
            eventListener.onEvent(new Event.ConfigurationFileGenerated(file));
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
        var json = new JsonWriter(writer);
        json.openObject();

        var foreign = metadata.foreign();
        json.memberName("foreign");
        json.openObject();
        writeDowncalls(json, foreign.downcalls());
        writeUpcalls(json, foreign.upcalls());
        json.closeObject();

        var reflection = metadata.reflection();
        if (reflection != null && !reflection.isEmpty()) {
            writeReflection(json, reflection);
        }

        json.closeObject();
        json.flush();
    }

    private void writeDowncalls(JsonWriter json, List<Downcall> downcalls) throws IOException {
        if (downcalls == null || downcalls.isEmpty())
            return;

        json.memberName("downcalls").openArray();
        for (var downcall : downcalls) {
            json.openObject().member("returnType", downcall.returnType());
            writeStringArray(json, "parameterTypes", downcall.parameterTypes());
            if (downcall.linkerOptions().captureCallState()) {
                json.memberName("options");
                json.openObject().member("captureCallState", true).closeObject();
            }
            json.closeObject();
        }
        json.closeArray();
    }

    private void writeUpcalls(JsonWriter json, List<Upcall> upcalls) throws IOException {
        if (upcalls == null || upcalls.isEmpty())
            return;

        json.memberName("upcalls").openArray();
        for (var upcall : upcalls) {
            json.openObject();
            json.member("returnType", upcall.returnType());
            writeStringArray(json, "parameterTypes", upcall.parameterTypes());
            json.closeObject();
        }
        json.closeArray();
    }

    private void writeReflection(JsonWriter json, List<ReflectionObject> reflection) throws IOException {
        json.memberName("reflection").openArray();
        for (var object : reflection) {
            json.openObject();
            json.member("type", object.type());
            writeReflectionMethods(json, object.methods());
            json.closeObject();
        }
        json.closeArray();
    }

    private void writeReflectionMethods(JsonWriter json, List<Method> methods) throws IOException {
        json.memberName("methods").openArray();
        for (var method : methods) {
            json.openObject();
            json.member("name", method.name());
            writeStringArray(json, "parameterTypes", method.parameterTypes());
            json.closeObject();
        }
        json.closeArray();
    }

    private void writeStringArray(JsonWriter json, String name, List<String> values) throws IOException {
        json.memberName(name);
        json.openArray();
        for (var value : values) {
            json.value(value);
        }
        json.closeArray();
    }
}
