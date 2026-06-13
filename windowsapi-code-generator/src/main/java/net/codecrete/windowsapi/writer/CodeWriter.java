//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.events.EventListener;
import net.codecrete.windowsapi.metadata.ComInterface;
import net.codecrete.windowsapi.metadata.Delegate;
import net.codecrete.windowsapi.metadata.EnumType;
import net.codecrete.windowsapi.metadata.Metadata;
import net.codecrete.windowsapi.metadata.Struct;
import net.codecrete.windowsapi.metadata.Type;

/**
 * Generates Java code for a given scope of types, functions, and constants.
 * <p>
 * The generated source files are handed to a {@link SourceFileSink}, which decides
 * whether they are written to disk ({@link FileSink}), discarded ({@link NullSink}),
 * or captured in memory ({@link CapturingSink}).
 * </p>
 */
public class CodeWriter extends JavaCodeWriter<Type> {

    private final StructCodeWriter structCodeWriter;
    private final EnumCodeWriter enumCodeWriter;
    private final FunctionCodeWriter functionCodeWriter;
    private final CallbackFunctionCodeWriter callbackFunctionCodeWriter;
    private final ConstantCodeWriter constantCodeWriter;
    private final ComInterfaceWriter comInterfaceWriter;

    /**
     * Creates a new instance.
     *
     * @param metadata      the metadata
     * @param sink          the destination for the generated source files
     * @param eventListener the event listener to notify about events
     */
    public CodeWriter(Metadata metadata, SourceFileSink sink, EventListener eventListener) {
        super(new GenerationContext(metadata, sink, eventListener));

        structCodeWriter = new StructCodeWriter(generationContext());
        enumCodeWriter = new EnumCodeWriter(generationContext());
        functionCodeWriter = new FunctionCodeWriter(generationContext());
        callbackFunctionCodeWriter = new CallbackFunctionCodeWriter(generationContext());
        constantCodeWriter = new ConstantCodeWriter(generationContext());
        comInterfaceWriter = new ComInterfaceWriter(generationContext());
    }

    /**
     * Sets the base package.
     * <p>
     * The base package is prepended to all package names derived from Microsoft's namespace.
     * The default is an empty string, i.e., no further package name is added.
     * </p>
     *
     * @param basePackage the base package name
     */
    public void setBasePackage(String basePackage) {
        generationContext().setBasePackage(basePackage);
    }

    /**
     * Sets if the downcall tracing code is generated.
     *
     * @param downcallTracing  {@code true} if it is generated, {@code false} otherwise.
     */
    public void setGenerateDowncallTracing(boolean downcallTracing) {
        generationContext().setGenerateDowncallTracing(downcallTracing);
    }

    /**
     * Writes the Java code for the specified scope of types, functions, and constants.
     *
     * @param scope the scope
     */
    public void write(Scope scope) {
        scope.getTransitiveTypeScope().forEach(this::writeType);
        scope.getFunctions().forEach(functionCodeWriter::writeFunctions);
        scope.getConstants().forEach(constantCodeWriter::writeConstants);
    }

    /**
     * Writes the Java code for all types, functions, and constants.
     * <p>
     * This method is used for tests.
     * </p>
     */
    public void writeAll() {
        var metadata = generationContext.metadata();
        metadata.types().forEach(this::writeType);

        metadata.namespaces().values().stream()
                .filter(n -> !n.methods().isEmpty())
                .forEach(namespace -> functionCodeWriter.writeFunctions(namespace, namespace.methods().values()));


        metadata.namespaces().values().stream()
                .filter(n -> !n.constants().isEmpty())
                .forEach(namespace -> constantCodeWriter.writeConstants(namespace, namespace.constants().values()));
    }

    private void writeType(Type type) {
        switch (type) {
            case Struct struct when struct.namespace() != null -> structCodeWriter.writeStructOrUnion(struct);
            case EnumType enumType -> enumCodeWriter.writeEnum(enumType);
            case Delegate delegate -> callbackFunctionCodeWriter.writeCallbackFunction(delegate);
            case ComInterface comInterface -> comInterfaceWriter.writeComInterface(comInterface);
            default -> {
                // nothing to do
            }
        }
    }
}
