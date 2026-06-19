//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.naming;

import net.codecrete.windowsapi.metadata.EnumType;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.Type;
import net.codecrete.windowsapi.metadata.TypeAlias;

import java.util.Locale;

/**
 * Maps Windows API metadata to the Java names and types used in the generated code.
 * <p>
 * This is the single source of truth for the generated package names, class names
 * and parameter/return types. It is shared by the Java source writers and the
 * GraalVM reachability metadata builder so that the metadata references the exact
 * names that are emitted as Java code.
 * </p>
 */
public final class JavaNaming {

    /**
     * Fully-qualified name of the FFM memory segment type used for all non-scalar types.
     */
    public static final String MEMORY_SEGMENT = "java.lang.foreign.MemorySegment";

    private JavaNaming() {
    }

    /**
     * Creates a valid Java class name for the given type name.
     * <p>
     * Both names are without package / namespace name.
     * </p>
     *
     * @param typeName the type name
     * @return the Java class name
     */
    public static String toJavaClassName(String typeName) {
        // "AVIStreamHeader" and "AVISTREAMHEADER" conflict with each other
        // when the associated Java file is created as they only differ in case.
        if (typeName.equals("AVISTREAMHEADER"))
            return "AVISTREAMHEADER_";
        return typeName;
    }

    /**
     * Converts the namespace name to a Java package name.
     *
     * @param basePackage the base package (maybe empty)
     * @param namespace   the namespace
     * @return the Java package name
     */
    public static String toJavaPackageName(String basePackage, String namespace) {
        var lowercaseNamespace = namespace.toLowerCase(Locale.ROOT);
        if (basePackage.isEmpty())
            return lowercaseNamespace;
        return basePackage + "." + lowercaseNamespace;
    }

    /**
     * Gets the Java type for the given primitive type.
     *
     * @param type the primitive type
     * @return the name of the Java type
     */
    public static String getPrimitiveJavaType(Primitive type) {
        return switch (type.kind()) {
            case INT64, UINT64, INT_PTR, UINT_PTR -> "long";
            case INT32, UINT32 -> "int";
            case UINT16, INT16, CHAR -> "short";
            case BYTE, SBYTE -> "byte";
            case SINGLE -> "float";
            case DOUBLE -> "double";
            case BOOL -> "boolean";
            default -> throw new AssertionError("Unexpected primitive type: " + type.name());
        };
    }

    /**
     * Gets the Java type for the given metadata type.
     * <p>
     * Type aliases are resolved. For enumerations, the base integer type
     * is used. For non-primitive types, the result will be {@code MemorySegment}.
     * </p>
     *
     * @param type the metadata type
     * @return the Java type (without package name for {@code MemorySegment})
     */
    public static String getJavaType(Type type) {
        return getJavaType(type, false);
    }

    /**
     * Gets the fully-qualified Java type for the given metadata type.
     * <p>
     * Behaves like {@link #getJavaType(Type)} but non-primitive types resolve to the
     * fully-qualified {@value #MEMORY_SEGMENT}, suitable for GraalVM reflection metadata.
     * </p>
     *
     * @param type the metadata type
     * @return the fully-qualified Java type
     */
    public static String getFullyQualifiedJavaType(Type type) {
        return getJavaType(type, true);
    }

    private static String getJavaType(Type type, boolean fullyQualified) {
        return switch (type) {
            case Primitive primitive -> getPrimitiveJavaType(primitive);
            case EnumType enumType -> getPrimitiveJavaType(enumType.baseType());
            case TypeAlias typeAlias -> getJavaType(typeAlias.aliasedType(), fullyQualified);
            default -> fullyQualified ? MEMORY_SEGMENT : "MemorySegment";
        };
    }
}
