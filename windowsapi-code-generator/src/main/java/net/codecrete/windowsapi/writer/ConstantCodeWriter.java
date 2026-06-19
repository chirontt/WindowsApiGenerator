//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.ConstantValue;
import net.codecrete.windowsapi.metadata.Namespace;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.Type;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.codecrete.windowsapi.naming.JavaNaming.getJavaType;

/**
 * Creates the Java code for constants.
 */
class ConstantCodeWriter extends JavaCodeWriter<Type> {

    private final CommentWriter commentWriter = new CommentWriter();

    /**
     * Creates a new instance.
     *
     * @param generationContext the code generation context
     */
    ConstantCodeWriter(GenerationContext generationContext) {
        super(generationContext);
    }

    /**
     * Creates a new file with the Java code for the specified constants.
     *
     * @param namespace the metadata namespace
     * @param constants the constants
     */
    void writeConstants(Namespace namespace, Collection<ConstantValue> constants) {
        var sortedConstants = constants.stream().sorted(Comparator.comparing(ConstantValue::name)).toList();
        withFile(namespace, null, "Constants", file -> writeConstantsContent(file, sortedConstants));
    }

    void writeConstantsContent(JavaSourceFile<Type> file, Collection<ConstantValue> constants) {
        var writer = file.writer();
        var needsArena = constants.stream().anyMatch(constant -> !(constant.value() instanceof Number));
        var hasGuids = constants.stream().anyMatch(constant -> constant.value() instanceof UUID);
        var hasPropertyKeys = constants.stream().anyMatch(ConstantCodeWriter::isPropertyKey);

        writer.printf("""
                package %s;

                import java.lang.foreign.*;

                /**
                 * Constants of namespace %s.
                 */
                public class Constants {
                """, file.packageName(), file.namespace().name());

        if (needsArena)
            writer.print("""
                        private static final Arena ARENA = Arena.ofAuto();

                    """);

        if (hasGuids)
            writeCreateGuidMethod(writer, 4);

        if (hasPropertyKeys)
            writer.print("""
                        private static MemorySegment createPropertyKey(long v1, long v2, int v3) {
                            var seg = ARENA.allocate(20, 4);
                            seg.set(ValueLayout.JAVA_LONG, 0, v1);
                            seg.set(ValueLayout.JAVA_LONG, 8, v2);
                            seg.set(ValueLayout.JAVA_INT, 16, v3);
                            return seg;
                        }

                    """);

        for (var constant : constants)
            writeConstant(writer, constant);

        writer.println("}");
    }

    private static final Set<String> POINTER_STRUCT_TYPES = Set.of("CONDITION_VARIABLE", "SRWLOCK", "INIT_ONCE");

    private void writeConstant(PrintWriter writer, ConstantValue constant) {
        var typeName = constant.type().name();

        switch (constant.value()) {
            case String ignored -> {
                if (isPropertyKey(constant)) {
                    writePropertyKey(writer, constant);
                } else if (typeName.equals("SID_IDENTIFIER_AUTHORITY")) {
                    writeByteArrayConstant(writer, constant);
                } else if (constant.name().equals("GUID_DATABASE_32K_PAGES_OPTIONAL_FEATURE_BYTE")) {
                    // Special case: this constant is defined as a string, but it is actually a byte array
                    writeUTF16ByteArrayConstant(writer, constant);
                } else if (POINTER_STRUCT_TYPES.contains(typeName)) {
                    writePointerStruct(writer, constant);
                } else {
                    writeStringConstant(writer, constant);
                }
            }
            case UUID ignored -> writeGuidConstant(writer, constant);
            case Number ignored -> writeNumericConstant(writer, constant);
            default -> throw new AssertionError("Unexpected constant type: " + constant.type());
        }
    }

    private void writeNumericConstant(PrintWriter writer, ConstantValue constant) {
        String typeName;
        if (constant.type() instanceof Primitive primitive)
            typeName = CommentWriter.getPrimitiveCType(primitive);
        else
            typeName = constant.type().name();

        commentWriter.writeConstantComment(writer, constant, "Numeric", typeName);

        writer.printf("    public static final %s %s = ", getJavaType(constant.type()), constant.name());
        writeValue(writer, constant.type(), constant.value());
        writer.println(";");
        writer.println();
    }

    private void writeStringConstant(PrintWriter writer, ConstantValue constant) {
        var value = constant.value().toString();
        // FFM does not support Windows-1252 charset.
        // Ensure the string is the same in UTF-8.
        assert !constant.isAnsiEncoding()
                || Arrays.compare(
                value.getBytes(StandardCharsets.UTF_8),
                value.getBytes(Charset.forName("windows-1252"))
        ) == 0;

        var stringType = String.format("%s, null-terminated", constant.isAnsiEncoding() ? "ANSI" : "UTF-16");
        writer.printf("    private static final MemorySegment %s$SEG = ARENA.allocateFrom(\"%s\"%s);%n%n",
                constant.name(),
                value
                        .replace("\\", "\\\\")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r"),
                !constant.isAnsiEncoding() ? ", java.nio.charset.StandardCharsets.UTF_16LE" : ""
        );

        commentWriter.writeConstantComment(writer, constant, "String", stringType);
        writeMemorySegmentConstant(writer, constant.name());
    }

    private void writeGuidConstant(PrintWriter writer, ConstantValue constant) {
        writeGuidConstantMemorySegment(writer, constant.name(), (UUID) constant.value(), 4);

        commentWriter.writeConstantComment(writer, constant, "GUID",
                String.format("{@code {%s}}", constant.value()));
        writeMemorySegmentConstant(writer, constant.name());
    }

    private static boolean isPropertyKey(ConstantValue constant) {
        return constant.type().name().equals("PROPERTYKEY") || constant.type().name().equals("DEVPROPKEY");
    }

    private void writePropertyKey(PrintWriter writer, ConstantValue constant) {
        assert constant.value() instanceof String;
        var numbers = parseNumbers((String) constant.value());
        assert numbers.length == 12;

        var data1 = numbers[0];
        var data2 = numbers[1] << 32;
        var data3 = numbers[2] << 48;
        var v1 = data1 | data2 | data3;

        var v2 = 0L;
        for (int i = 10; i >= 3; i--)
            v2 = (v2 << 8) | numbers[i];

        var v3 = numbers[11];

        writer.printf("""
                    private static final MemorySegment %s$SEG = createPropertyKey(%dL, %dL, %d);

                """, constant.name(), v1, v2, v3);

        commentWriter.writeConstantComment(writer, constant, "Property key", null);
        writeMemorySegmentConstant(writer, constant.name());
    }

    private void writeByteArrayConstant(PrintWriter writer, ConstantValue constant) {
        var numbers = parseNumbers(constant.value().toString());
        writeByteArrayConstant(writer, constant, numbers);
    }

    private void writeUTF16ByteArrayConstant(PrintWriter writer, ConstantValue constant) {
        // writes a byte array that has been encoded as a UTF-16 string
        var bytes = constant.value().toString().chars().asLongStream().toArray();
        writeByteArrayConstant(writer, constant, bytes);
    }

    private void writeByteArrayConstant(PrintWriter writer, ConstantValue constant, long[] bytes) {
        writer.printf("    private static final MemorySegment %s$SEG = ARENA.allocateFrom(ValueLayout.JAVA_BYTE",
                constant.name());
        for (var b : bytes) {
            writer.print(", (byte) ");
            writer.print(b);
        }
        writer.println(");");
        writer.println();

        commentWriter.writeConstantComment(writer, constant, "Binary", null);
        writeMemorySegmentConstant(writer, constant.name());
    }

    private void writePointerStruct(PrintWriter writer, ConstantValue constant) {
        // The struct consists of a single pointer (address).
        writer.printf("""
                    private static final MemorySegment %s$SEG = ARENA.allocateFrom(ValueLayout.JAVA_LONG, %sL);

                """, constant.name(), constant.value());

        commentWriter.writeConstantComment(writer, constant, constant.type().name(), null);
        writeMemorySegmentConstant(writer, constant.name());
    }

    private static long[] parseNumbers(String value) {
        var numbers = value
                .replace('{', ' ')
                .replace('}', ' ')
                .replace(" ", "")
                .split(",");
        return Arrays.stream(numbers).map(Long::parseLong).mapToLong(Long::longValue).toArray();
    }

    private void writeMemorySegmentConstant(PrintWriter writer, String name) {
        writer.printf("""
                    public static MemorySegment %1$s() {
                        return %1$s$SEG;
                    }

                """, name);
    }
}
