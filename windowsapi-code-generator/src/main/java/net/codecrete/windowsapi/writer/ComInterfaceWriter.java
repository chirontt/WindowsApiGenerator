//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.ComInterface;
import net.codecrete.windowsapi.metadata.EnumType;
import net.codecrete.windowsapi.metadata.Method;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.PrimitiveKind;
import net.codecrete.windowsapi.metadata.Type;
import net.codecrete.windowsapi.metadata.TypeAlias;

import static net.codecrete.windowsapi.naming.JavaNaming.toJavaClassName;

/**
 * Creates the Java code for COM interfaces.
 */
@SuppressWarnings("resource")
class ComInterfaceWriter extends FunctionCodeWriterBase<ComInterface> {

    private final CommentWriter commentWriter = new CommentWriter();

    /**
     * Creates a new instance.
     *
     * @param generationContext the code generation context
     */
    ComInterfaceWriter(GenerationContext generationContext) {
        super(generationContext);
    }

    /**
     * Creates a new file with the Java code for the specified COM interface.
     *
     * @param comInterface the COM interface
     */
    void writeComInterface(ComInterface comInterface) {
        var name = toJavaClassName(comInterface.name());
        withFile(comInterface.namespace(), comInterface, name, this::writeComInterfaceContent);
    }

    private void writeComInterfaceContent(JavaSourceFile<ComInterface> file) {
        var writer = file.writer();
        var type = file.type();

        writer.printf("""
                package %s;

                import java.lang.foreign.*;
                import java.lang.invoke.*;
                import static java.lang.foreign.ValueLayout.*;

                """, file.packageName());

        writeComInterfaceComment(file);

        var extendsInterface = "";
        var implementedInterface = type.implementedInterface();
        if (implementedInterface != null) {
            extendsInterface = toJavaClassName(implementedInterface.name());
            if (type.namespace() != implementedInterface.namespace())
                extendsInterface =
                        toJavaPackageName(implementedInterface.namespace().name()) + "." + extendsInterface;
            extendsInterface = " extends " + extendsInterface;
        }
        writer.printf("""
                public interface %s%s {
                """, file.className(), extendsInterface);

        var methodNames = getAllMethodNames(type);
        writeComInterfaceMethods(file, methodNames);

        writeCommonMethods(file, methodNames);

        writeAddressLayouts(file);

        writeIidInnerClass(file);

        writeDowncallWrapper(file, methodNames, extendsInterface);

        final var methodOffset = getNumSuperMethods(type);
        for (int i = 0; i < type.methods().size(); i++) {
            var method = type.methods().get(i);
            var methodIndex = methodOffset + i;
            var innerClassName = "VFUNC" + methodIndex;
            writeFunctionInnerClass(file, method, innerClassName);
        }

        writeUpcallWrapper(file, methodNames);
        writeUpcallImplementation(file, methodNames);

        writer.println("}");
    }

    private void writeComInterfaceMethods(JavaSourceFile<ComInterface> file, String[] methodNames) {
        var writer = file.writer();
        var type = file.type();
        final var methodOffset = getNumSuperMethods(type);
        for (int i = 0; i < type.methods().size(); i++) {
            var method = type.methods().get(i);
            var methodName = methodNames[methodOffset + i];

            commentWriter.writeFunctionComment(writer, method, "COM interface method");

            writer.print("    ");
            writeFunctionSignatureIntro(writer, method, methodName);
            writeFunctionSignatureParameters(writer, method);
            writer.println(";");
            writer.println();
        }
    }

    private void writeAddressLayouts(JavaSourceFile<ComInterface> file) {
        var writer = file.writer();
        var type = file.type();
        final var methodOffset = getNumSuperMethods(type);

        writer.printf("""
                    StructLayout %1$s$COM_OBJECT_LAYOUT = MemoryLayout.structLayout(
                        ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(%2$d, ADDRESS)).withName("vtable")
                    );

                    AddressLayout %1$s$ADDRESS_LAYOUT = ADDRESS.withTargetLayout(%1$s$COM_OBJECT_LAYOUT);

                """, file.className(), methodOffset + type.methods().size());

        AddressLayout.requiredLayouts(type).forEach(layoutType ->
                writeAddressLayoutInitialization(writer, layoutType, ""));
        writer.println();

    }

    private void writeIidInnerClass(JavaSourceFile<ComInterface> file) {
        var writer = file.writer();
        var type = file.type();
        if (type.getIid() == null)
            return;

        writer.printf("""
                    class $IID$%1$s {
                """, file.className());

        writer.print("""
                        private static final Arena ARENA = Arena.ofAuto();

                """);

        writeCreateGuidMethod(writer, 8);
        writeGuidConstantMemorySegment(writer, "IID", type.getIid(), 8);

        writer.print("""
                    }

                """);
    }

    private void writeCommonMethods(JavaSourceFile<ComInterface> file, String[] methodNames) {
        var writer = file.writer();
        var type = file.type();
        var className = file.className();

        writeComment(writer, "Wraps the given COM object in a Java object with methods to call the COM interface functions.");
        writer.printf("""
                    static %s wrap(MemorySegment comObject) {
                        return new $DOWNCALL(comObject.reinterpret(8L));
                    }

                """, className);

        writeComment(writer, "Gets the address layout for pointers to {@code %s} COM interfaces.", type.name());
        writer.printf("""
                    static AddressLayout addressLayout() {
                        return %1$s$ADDRESS_LAYOUT;
                    }

                """, className);

        if (type.getIid() != null) {
            writeComment(writer, "Interface identifier (IID) for {@code %s} ({@code {%s}}).", type.name(), type.getIid());
            writer.printf("""
                        static MemorySegment iid() {
                            return $IID$%1$s.IID$SEG;
                        }

                    """, className);
        }

        writeComment(writer, "Creates a COM object instance for the given Java object implementing {@code %s}.",
                toJavaClassName(type.name()));
        writer.printf("""
                    static MemorySegment create(%1$s obj, Arena arena) {
                        var unwrapper = new $UPCALL(obj);
                        var vtable = arena.allocate(ADDRESS, %2$d);
                        var linker = Linker.nativeLinker();
                        for (int i = 0; i < %2$d; i++)
                            vtable.set(ADDRESS, 8 * i, linker.upcallStub($UPCALL_IMPL.HANDLES[i].bindTo(unwrapper), $UPCALL_IMPL.DESCRIPTORS[i], arena));
                        var objSegment = arena.allocate(ADDRESS);
                        objSegment.set(ADDRESS, 0, vtable);
                        return objSegment;
                    }

                """, toJavaClassName(type.name()), methodNames.length);
    }

    private void writeDowncallWrapper(JavaSourceFile<ComInterface> file, String[] methodNames, String extendsInterface) {
        var writer = file.writer();
        var type = file.type();
        var className = file.className();
        final var methodOffset = getNumSuperMethods(type);
        var implementedInterface = type.implementedInterface();

        var extendsSuperClass = "";
        if (!extendsInterface.isEmpty())
            extendsSuperClass = extendsInterface + ".$DOWNCALL";

        writer.printf("""
                    class $DOWNCALL%2$s implements %1$s {
                        private static final VarHandle VTABLE_FUNC_VARHANDLE = %1$s$COM_OBJECT_LAYOUT.varHandle(
                                MemoryLayout.PathElement.groupElement("vtable"),
                                MemoryLayout.PathElement.dereferenceElement(),
                                MemoryLayout.PathElement.sequenceElement()
                        );

                        private static MemorySegment vtableFunc(MemorySegment comObject, long index) {
                            return (MemorySegment) VTABLE_FUNC_VARHANDLE.get(comObject, 0L, index);
                        }

                """, className, extendsSuperClass);

        writeTraceDowncallHeader(writer, "        ");

        if (implementedInterface != null) {
            writer.printf("""
                            protected $DOWNCALL(MemorySegment comObject) {
                                super(comObject);
                            }

                    """);
        } else {
            writer.printf("""
                            protected final MemorySegment comObject;

                            protected $DOWNCALL(MemorySegment comObject) {
                                this.comObject = comObject;
                            }

                    """);
        }

        for (int i = 0; i < type.methods().size(); i++) {
            var method = type.methods().get(i);
            var methodIndex = methodOffset + i;
            var methodName = methodNames[methodIndex];
            var innerClassName = "VFUNC" + methodIndex;

            writer.print("        public ");
            writeFunctionSignatureIntro(writer, method, methodName);
            writeFunctionSignatureParameters(writer, method);
            writer.println(" {");
            var invokeString = innerClassName + "$IMPL.HANDLE.invokeExact(vtableFunc(comObject, " + methodIndex + ")," +
                    " comObject";
            if (hasParameters(method))
                invokeString += ", ";
            writeInvoke(writer, method, invokeString, 12);
            writer.println("        }");
            writer.println();
        }

        writer.println("    }");
        writer.println();
    }

    private void writeFunctionInnerClass(JavaSourceFile<ComInterface> file, Method method, String methodName) {
        var writer = file.writer();
        // start of inner class
        writer.printf("""
                    class %s$IMPL {
                """, methodName);

        // function descriptor
        writer.print("        private static final FunctionDescriptor DESC = ");
        writeFunctionDescriptor(writer, method, file.className() + "$ADDRESS_LAYOUT");
        writer.println(";");

        // method handle
        writer.print("""
                        private static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(DESC);
                    }

                """);
    }

    private void writeUpcallWrapper(JavaSourceFile<ComInterface> file, String[] methodNames) {
        var writer = file.writer();
        var type = file.type();
        var numMethods = methodNames.length;
        var methods = getAllMethods(type);

        writer.printf("""
                    class $UPCALL {
                        private final %1$s javaObject;

                        private $UPCALL(%1$s javaObject) {
                            this.javaObject = javaObject;
                        }

                """, file.className());

        // Methods to be used for upcall stubs.
        // Each function calls the matching function of the wrapper Java object.
        // The matching functions do not have the COM object pointer as the first parameter.
        for (int i = 0; i < numMethods; i++) {
            var method = methods[i];
            var methodName = methodNames[i];
            writer.print("        ");
            writeFunctionSignatureIntro(writer, method, methodName);
            writer.print("MemorySegment thisObject");
            if (hasParameters(method))
                writer.print(", ");
            writeFunctionSignatureParameters(writer, method);
            writer.println(" {");
            writer.print("            ");
            if (method.hasReturnType())
                writer.print("return ");
            writer.printf("javaObject.%s(", methodNames[i]);
            writeInvocationArguments(writer, method);
            writer.println(");");
            writer.println("        }");
        }

        writer.print("""
                    }

                """);
    }

    private void writeUpcallImplementation(JavaSourceFile<ComInterface> file, String[] methodNames) {
        var writer = file.writer();
        var numMethods = methodNames.length;
        var methods = getAllMethods(file.type());

        // Helper class to create upcall wrappers
        writer.printf("""
                    class $UPCALL_IMPL {
                        private static final FunctionDescriptor[] DESCRIPTORS = createDescriptors();
                        private static final MethodHandle[] HANDLES = createHandles();

                        private static FunctionDescriptor[] createDescriptors() {
                            var descriptors = new FunctionDescriptor[%d];
                """, numMethods);

        for (int i = 0; i < numMethods; i++) {
            writer.printf("            descriptors[%d] = ", i);
            writeFunctionDescriptor(writer, methods[i], "ADDRESS");
            writer.println(";");
        }

        writer.printf("""
                            return descriptors;
                        }

                        private static MethodHandle[] createHandles() {
                            try {
                                var lookup = MethodHandles.lookup();
                                var handles = new MethodHandle[%d];
                """, numMethods);

        for (int i = 0; i < numMethods; i++) {
            writer.printf("""
                                    handles[%1$d] = lookup.findVirtual($UPCALL.class, "%2$s", DESCRIPTORS[%1$d].toMethodType());
                    """, i, methodNames[i]);
        }

        writer.print("""
                               return handles;
                           } catch (ReflectiveOperationException e) {
                               throw new RuntimeException(e);
                           }
                       }
                   }
                """);
    }

    /**
     * Gets the number of methods implemented by super classes / interfaces.
     */
    private static int getNumSuperMethods(ComInterface comInterface) {
        var methodCount = 0;
        var superInterface = comInterface.implementedInterface();
        while (superInterface != null) {
            methodCount += superInterface.methods().size();
            superInterface = superInterface.implementedInterface();
        }
        return methodCount;
    }

    /**
     * Collects the methods of the provided COM interface and its super interfaces into the provided array.
     *
     * @param comInterface the COM interface
     * @param methods      the method array
     * @return the number of collected methods
     */
    private static int collectMethods(ComInterface comInterface, Method[] methods) {
        int index = 0;
        if (comInterface.implementedInterface() != null)
            index = collectMethods(comInterface.implementedInterface(), methods);
        for (var method : comInterface.methods()) {
            methods[index] = method;
            index += 1;
        }
        return index;
    }

    /**
     * Gets an array of all methods (including those of super interfaces)
     *
     * @param comInterface the COM interface
     * @return the method array
     */
    private static Method[] getAllMethods(ComInterface comInterface) {
        var methodCount = getNumSuperMethods(comInterface) + comInterface.methods().size();
        var methods = new Method[methodCount];
        collectMethods(comInterface, methods);
        return methods;
    }

    /**
     * Gets all method names of the specified COM interface (including those of super interfaces).
     * <p>
     * If a method overloading conflict occurs in Java,
     * the conflicting methods are renamed.
     * </p>
     *
     * @param comInterface the COM interface
     * @return the method name array
     */
    private static String[] getAllMethodNames(ComInterface comInterface) {
        var methodCount = getNumSuperMethods(comInterface) + comInterface.methods().size();
        var methodNames = new String[methodCount];
        collectMethodNames(comInterface, methodNames);
        return methodNames;
    }

    /**
     * Collects the method names of the provided COM interface and its super interfaces into the provided array.
     * <p>
     * COM interfaces allow method name overloading as does Java. However, some method signatures that are
     * different in C++ are the same in the generated Java FFM code. So, they result in an ambiguous overload
     * unless the method is renamed. This function takes care of it.
     * </p>
     *
     * @param comInterface the COM interface
     * @param methodNames  the method name array
     * @return the number of collected methods
     */
    @SuppressWarnings("java:S3776")
    private static int collectMethodNames(ComInterface comInterface, String[] methodNames) {
        var methods = getAllMethods(comInterface);

        int numSuperMethods = 0;
        if (comInterface.implementedInterface() != null)
            numSuperMethods = collectMethodNames(comInterface.implementedInterface(), methodNames);

        // calculate signature keys
        var signatureKeys = new long[methods.length];
        for (int i = 0; i < methods.length; i++)
            signatureKeys[i] = getSignatureKey(methods[i]);

        // detect and rename overloading conflicts of the COM interface
        // (super interfaces are considered but not modified)
        int numOwnMethods = comInterface.methods().size();
        for (int i = numSuperMethods; i < numSuperMethods + numOwnMethods; i++) {
            if (methodNames[i] != null)
                continue; // already done
            var methodName = methods[i].name();
            var signatureKey = getSignatureKey(methods[i]);
            var numSameSignature = 0;
            var currentMethodIndex = -1;

            for (int j = 0; j < signatureKeys.length; j++) {
                if (signatureKeys[j] == signatureKey && methodName.equals(methods[j].name())) {
                    numSameSignature++;
                    if (j > i)
                        methodNames[j] = methodName + numSameSignature;
                    else if (j == i)
                        currentMethodIndex = numSameSignature;
                }
            }

            methodNames[i] = numSameSignature == 1 ? methodName : methodName + currentMethodIndex;
        }

        return numSuperMethods + numOwnMethods;
    }

    /**
     * Calculates a unique numeric method signature key.
     *
     * @param method the method
     * @return signature key
     */
    private static long getSignatureKey(Method method) {
        var index = (long) method.parameters().length;
        for (var param : method.parameters()) {
            index = (index << 4) | getJavaTypeKey(param.type());
        }
        return index;
    }

    private static long getJavaTypeKey(Type paramType) {
        return switch (paramType) {
            case Primitive primitive -> getPrimitiveJavaTypeIndex(primitive);
            case EnumType enumType -> getPrimitiveJavaTypeIndex(enumType.baseType());
            case TypeAlias typeAlias -> getJavaTypeKey(typeAlias.aliasedType());
            default -> 0;
        };
    }

    private static long getPrimitiveJavaTypeIndex(Primitive primitive) {
        return switch (primitive.kind()) {
            case PrimitiveKind.INT64, PrimitiveKind.UINT64, PrimitiveKind.INT_PTR, PrimitiveKind.UINT_PTR -> 1;
            case PrimitiveKind.INT32, PrimitiveKind.UINT32 -> 2;
            case PrimitiveKind.UINT16, PrimitiveKind.INT16, PrimitiveKind.CHAR -> 3;
            case PrimitiveKind.BYTE, PrimitiveKind.SBYTE -> 4;
            case PrimitiveKind.SINGLE -> 5;
            case PrimitiveKind.DOUBLE -> 6;
            case PrimitiveKind.BOOL -> 7;
            default -> throw new AssertionError("Unexpected primitive type: " + primitive.name());
        };
    }

    private void writeComInterfaceComment(JavaSourceFile<ComInterface> file) {
        var writer = file.writer();
        writer.printf("""
                /**
                 * {@code %s} COM interface
                """, file.type().name());

        writeDocumentationUrl(writer, file.type());

        writer.println(" */");
    }
}
