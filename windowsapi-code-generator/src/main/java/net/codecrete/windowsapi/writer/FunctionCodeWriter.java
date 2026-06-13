//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.Method;
import net.codecrete.windowsapi.metadata.Namespace;
import net.codecrete.windowsapi.metadata.Pointer;
import net.codecrete.windowsapi.metadata.Type;
import net.codecrete.windowsapi.metadata.TypeAlias;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Creates the Java code for the functions in a given namespace.
 */
@SuppressWarnings("resource")
class FunctionCodeWriter extends FunctionCodeWriterBase<Type> {

    private static final String CALL_STATE_NOTE_1 = "The additional first parameter takes a segment allocator to " +
            "allocate the struct return values.";
    private static final String CALL_STATE_NOTE_2 = "The additional %s parameter takes a memory segment to capture " +
            "the call state (replacement for {@code GetLastError()}).";

    private final CommentWriter commentWriter = new CommentWriter();

    /**
     * Creates a new instance.
     *
     * @param generationContext the code generation context
     */
    FunctionCodeWriter(GenerationContext generationContext) {
        super(generationContext);
    }

    /**
     * Writes an "Apis" class with the specified functions.
     *
     * @param functions functions to write
     */
    void writeFunctions(Namespace namespace, Collection<Method> functions) {
        var sortedFunctions = functions.stream().sorted(Comparator.comparing(Method::name)).toList();
        withFile(namespace, null, "Apis", file -> writeFunctionsContent(file, sortedFunctions));
    }

    void writeFunctionsContent(JavaSourceFile<Type> file, Collection<Method> functions) {
        var writer = file.writer();

        writer.printf("""
                package %s;

                import java.lang.foreign.*;
                import java.lang.invoke.MethodHandle;
                import static java.lang.foreign.ValueLayout.*;

                """, file.packageName());

        writeApiComment(file);

        writer.print("""
                public class Apis {

                """);

        writer.print("""
                static {
                    var libraryPath = System.getenv("WINDIR") + "\\\\SYSTEM32\\\\";
            """);
        functions.stream().map(Method::dll).filter(Objects::nonNull).distinct().sorted().forEach(dll ->
                writer.printf("""
                            System.load(libraryPath + "%s.dll");
                    """, dllName(dll)));
        writer.print("""
                }

            """);

        boolean usesLastError = anyFunctionUsesLastError(functions);

        writer.print("""
                    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();
                    private static final Linker LINKER = Linker.nativeLinker();
                """);
        if (usesLastError)
            writer.print("""
                        private static final Linker.Option LAST_ERROR_STATE = Linker.Option.captureCallState("GetLastError");
                    """);

        AddressLayout.requiredLayouts(functions).forEach(layoutType ->
                writeAddressLayoutInitialization(writer, layoutType, "private static final "));

        writer.println();

        writeTraceDowncallHeader(writer, "    ");

        for (var method : functions)
            writeFunction(writer, method);

        writer.println("}");
    }

    private boolean anyFunctionUsesLastError(Collection<Method> functions) {
        return functions.stream().anyMatch(Method::supportsLastError);
    }

    private void writeFunction(PrintWriter writer, Method method) {
        var isInlined = method.dll() == null;

        if (isInlined) {
            assert method.constantValue() != null;
        } else {
            writeFunctionInnerClass(writer, method);
            writeFunctionDescriptorAndHandle(writer, method);
        }

        // function
        commentWriter.writeFunctionComment(writer, method, "function");

        var methodName = method.name();
        writer.print("    public static ");
        writeFunctionSignature(writer, method, methodName);
        writer.println(" {");

        if (isInlined) {
            assert method.constantValue() instanceof String;
            assert method.returnType() instanceof TypeAlias typeAlias && typeAlias.aliasedType() instanceof Pointer;
            writer.printf("        return MemorySegment.ofAddress(%s);", method.constantValue());
        } else {
            writeInvoke(writer, method, methodName + "$IMPL.HANDLE.invokeExact(", 8);
        }

        writer.println("    }");
        writer.println();
    }

    private void writeFunctionInnerClass(PrintWriter writer, Method method) {
        var methodName = method.name();

        // start of inner class and function descriptor
        writer.printf("    private static class %s$IMPL {%n", methodName);
        writer.print("        private static final FunctionDescriptor DESC = ");
        writeFunctionDescriptor(writer, method, null);
        writer.println(";");

        // method handle and end of inner class
        writer.printf("""
                                private static final MethodHandle HANDLE = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("%s"), DESC%s);
                            }

                        """,
                method.nativeName(),
                method.supportsLastError() ? ", LAST_ERROR_STATE" : "");
    }

    private void writeFunctionDescriptorAndHandle(PrintWriter writer, Method method) {
        var methodName = method.name();
        var supportsAllocator = method.supportsAllocator();
        var supportsLastError = method.supportsLastError();

        var callStateNote1 = supportsAllocator ? CALL_STATE_NOTE_1 : null;
        var position = supportsAllocator ? "second" : "first";
        var callStateNote2 = supportsLastError ? String.format(CALL_STATE_NOTE_2, position) : null;

        // descriptor accessor
        writeCommentWithNotes(writer, String.format("Gets the function descriptor for {@code %s}", method.nativeName()),
                callStateNote1, callStateNote2);
        writer.printf("""
                    public static FunctionDescriptor %1$s$descriptor() {
                        return %1$s$IMPL.DESC;
                    }

                """, methodName);

        // handle accessor
        writeComment(writer, "Gets the method handle for {@code %s}", method.nativeName());
        writer.printf("""
                    public static MethodHandle %1$s$handle() {
                        return %1$s$IMPL.HANDLE;
                    }

                """, methodName);
    }

    private static String dllName(String dll) {
        if (dll.length() > 4) {
            var suffix = dll.substring(dll.length() - 4);
            if (".dll".equalsIgnoreCase(suffix))
                return dll.substring(0, dll.length() - 4);
        }
        return dll;
    }

    void writeApiComment(JavaSourceFile<Type> file) {
        file.writer().printf("""
                /**
                 * Functions of namespace {@code %s}
                 */
                """, file.namespace().name());
    }
}
