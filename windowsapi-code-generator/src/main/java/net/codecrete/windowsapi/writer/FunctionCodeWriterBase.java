//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.Method;
import net.codecrete.windowsapi.metadata.Type;

import java.io.PrintWriter;

/**
 * Base class for code writers generating function descriptors and calls.
 *
 * @param <T> the metadata type
 */
class FunctionCodeWriterBase<T extends Type> extends JavaCodeWriter<T> {

    /**
     * Creates a new instance.
     *
     * @param generationContext the code generation context
     */
    protected FunctionCodeWriterBase(GenerationContext generationContext) {
        super(generationContext);
    }

    /**
     * Writes the Java code for creating a function descriptor.
     *
     * @param writer        the print writer
     * @param method        the function
     * @param thisParameter the additional {@code this} parameter (or {@code null})
     */
    protected void writeFunctionDescriptor(PrintWriter writer, Method method, String thisParameter) {
        writer.print("FunctionDescriptor.");
        if (method.hasReturnType()) {
            writer.print("of(");
            writer.print(getLayoutName(method.returnType(), null));
        } else {
            writer.print("ofVoid(");
        }

        if (thisParameter != null) {
            if (method.hasReturnType())
                writer.print(", ");
            writer.print(thisParameter);
        }

        var parameters = method.parameters();
        for (int i = 0; i < parameters.length; i += 1) {
            writer.print(i > 0 || thisParameter != null || method.hasReturnType() ? ", " : "");
            writer.print(getLayoutName(parameters[i].type(), null));
        }
        writer.print(")");
    }

    /**
     * Writes the Java method signature for the given function.
     *
     * @param writer       the print writer
     * @param function     the function
     * @param functionName the function name
     */
    protected static void writeFunctionSignature(PrintWriter writer, Method function, String functionName) {
        writeFunctionSignatureIntro(writer, function, functionName);
        writeFunctionSignatureParameters(writer, function);
    }

    /**
     * Writes the Java method signature intro (return type, function name, opening parenthesis)
     *
     * @param writer       the print writer
     * @param function     the function
     * @param functionName the function name
     */
    protected static void writeFunctionSignatureIntro(PrintWriter writer, Method function, String functionName) {
        writer.printf("%s %s(",
                function.hasReturnType() ? getJavaType(function.returnType()) : "void",
                functionName);
    }

    /**
     * Writes the parameters of the Java method signature (without opening and closing parentheses)
     *
     * @param writer   the print writer
     * @param function the function
     */
    protected static void writeFunctionSignatureParameters(PrintWriter writer, Method function) {
        var parameters = function.parameters();
        var supportsAllocator = function.supportsAllocator();
        var supportsLastError = function.supportsLastError();

        if (supportsAllocator)
            writer.print("SegmentAllocator allocator");
        if (supportsLastError)
            writer.printf("%sMemorySegment lastErrorState",
                    supportsAllocator ? ", " : "");

        for (int i = 0; i < parameters.length; i += 1) {
            writer.printf("%s%s %s",
                    i > 0 || supportsAllocator || supportsLastError ? ", " : "",
                    getJavaType(parameters[i].type()),
                    getJavaSafeName(parameters[i].name()));
        }
        writer.print(")");
    }

    /**
     * Writes the Java code for invoking a native function through a method handle.
     *
     * @param writer    the print writer
     * @param function  the function
     * @param invoke    the name of the method handle to invoke
     * @param indenting the indenting (number of spaces)
     */
    protected void writeInvoke(PrintWriter writer, Method function, String invoke, int indenting) {
        var indent = getIndent(indenting);
        var returnWithCast = function.hasReturnType()
                ? String.format("return (%s) ", getJavaType(function.returnType()))
                : "";

        writer.printf("""
                %1$stry {
                """, indent);

        if (generationContext().generateDowncallTracing()) {
            writer.printf("""
                %1$s    if (TRACE_DOWNCALLS) {
                %1$s        traceDowncall("%2$s\"""", indent, function.name());
            writer.print(hasParameters(function) ? ", " : "");
            writeInvocationArguments(writer, function);
            writer.println(");");
            writer.printf("""
                    %1$s    }
                    """, indent);
        }

        writer.printf("%1$s    %2$s%3$s", indent, returnWithCast, invoke);

        writeInvocationArguments(writer, function);
        writer.println(");");

        writer.printf("""
                %1$s} catch (Throwable ex) {
                %1$s    throw new RuntimeException(ex);
                %1$s}
                """, indent);
    }

    /**
     * Writes the Java invocation arguments for the given function (without opening parenthesis).
     *
     * @param writer   the print writer
     * @param function the function
     */
    protected static void writeInvocationArguments(PrintWriter writer, Method function) {
        var supportsAllocator = function.supportsAllocator();
        var supportsLastError = function.supportsLastError();
        if (supportsAllocator)
            writer.print("allocator");
        if (supportsLastError)
            writer.printf("%slastErrorState", supportsAllocator ? ", " : "");

        var parameters = function.parameters();
        for (int i = 0; i < parameters.length; i += 1) {
            writer.print(i > 0 || supportsAllocator || supportsLastError ? ", " : "");
            writer.print(getJavaSafeName(parameters[i].name()));
        }
    }

    protected static boolean hasParameters(Method method) {
        return method.parameters().length > 0 || method.supportsLastError() || method.supportsAllocator();
    }

    protected void writeTraceDowncallHeader(PrintWriter writer, String indent) {
        if (!generationContext().generateDowncallTracing())
            return;

        writer.printf("""
            %1$sprivate static final boolean TRACE_DOWNCALLS = Boolean.getBoolean("windowsapi.trace.downcalls");

            %1$sprivate static void traceDowncall(String name, Object... args) {
            %1$s    var traceArgs = java.util.Arrays.stream(args)
            %1$s            .map(Object::toString)
            %1$s            .collect(java.util.stream.Collectors.joining(", "));
            %1$s    System.writer.printf("%%s(%%s)%%n", name, traceArgs);
            %1$s}

            """, indent);
    }
}
