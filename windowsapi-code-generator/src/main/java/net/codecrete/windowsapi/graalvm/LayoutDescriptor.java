//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.graalvm;

import net.codecrete.windowsapi.metadata.Array;
import net.codecrete.windowsapi.metadata.ComInterface;
import net.codecrete.windowsapi.metadata.Delegate;
import net.codecrete.windowsapi.metadata.EnumType;
import net.codecrete.windowsapi.metadata.Method;
import net.codecrete.windowsapi.metadata.Pointer;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.Struct;
import net.codecrete.windowsapi.metadata.Type;
import net.codecrete.windowsapi.metadata.TypeAlias;
import net.codecrete.windowsapi.winmd.LayoutRequirement;

/**
 * Renders metadata types to GraalVM reachability metadata layout descriptors.
 * <p>
 * Each type is rendered to a textual descriptor using the JNI type names for
 * scalar leaf types ({@code jint}, {@code jlong}, ...), {@code void*} for
 * pointers, and the composite constructors {@code struct(...)}, {@code union(...)},
 * {@code sequence(N, ...)}, {@code padding(N)} and {@code align(N, ...)} for
 * aggregates. The traversal mirrors the FFM memory layout generated for the
 * Java sources, but aggregates are always fully inlined.
 * </p>
 */
final class LayoutDescriptor {

    /**
     * Descriptor for a pointer (including function pointers and COM interfaces).
     */
    static final String POINTER = "void*";

    /**
     * Descriptor for a {@code void} return type.
     */
    static final String VOID = "void";

    private LayoutDescriptor() {
    }

    /**
     * Renders the return type of a method.
     *
     * @param method the method
     * @return the descriptor, or {@value #VOID} if the method does not return a value
     */
    static String ofReturnType(Method method) {
        return method.hasReturnType() ? of(method.returnType()) : VOID;
    }

    /**
     * Renders the given type to a layout descriptor.
     *
     * @param type the type
     * @return the descriptor
     */
    static String of(Type type) {
        var builder = new StringBuilder();
        append(builder, type);
        return builder.toString();
    }

    private static void append(StringBuilder builder, Type type) {
        switch (type) {
            case Primitive primitive -> builder.append(jniType(primitive));
            case EnumType enumType -> builder.append(jniType(enumType.baseType()));
            case TypeAlias typeAlias -> append(builder, typeAlias.aliasedType());
            case Pointer ignored -> builder.append(POINTER);
            case Delegate ignored -> builder.append(POINTER);
            case ComInterface ignored -> builder.append(POINTER);
            case Array array -> appendArray(builder, array);
            case Struct struct -> appendStruct(builder, struct);
        }
    }

    private static void appendArray(StringBuilder builder, Array array) {
        builder.append("sequence(").append(array.arrayLength()).append(", ");
        append(builder, array.itemType());
        builder.append(')');
    }

    private static void appendStruct(StringBuilder builder, Struct struct) {
        var packed = isPacked(struct);
        if (packed)
            builder.append("align(").append(struct.packageSize()).append(", ");

        builder.append(struct.isUnion() ? "union(" : "struct(");
        var first = true;
        for (var member : struct.members()) {
            if (!first)
                builder.append(", ");
            first = false;
            append(builder, member.type());
            if (!packed && member.paddingAfter() > 0)
                builder.append(", padding(").append(member.paddingAfter()).append(')');
        }
        builder.append(')');

        if (packed)
            builder.append(')');
    }

    /**
     * Indicates if the aggregate is packed, i.e., its alignment is smaller than its natural alignment.
     *
     * @param struct the struct or union
     * @return {@code true} if packed
     */
    private static boolean isPacked(Struct struct) {
        return struct.packageSize() < naturalAlignment(struct);
    }

    private static int naturalAlignment(Struct struct) {
        var alignment = 1;
        for (var member : struct.members())
            alignment = Math.max(alignment, LayoutRequirement.forType(member.type()).alignment());
        return alignment;
    }

    private static String jniType(Primitive primitive) {
        return switch (primitive.kind()) {
            case BOOL -> "jboolean";
            case CHAR -> "jchar";
            case BYTE, SBYTE -> "jbyte";
            case INT16, UINT16 -> "jshort";
            case INT32, UINT32 -> "jint";
            case INT64, UINT64, INT_PTR, UINT_PTR -> "jlong";
            case SINGLE -> "jfloat";
            case DOUBLE -> "jdouble";
            default -> throw new AssertionError("Unexpected primitive type: " + primitive.name());
        };
    }
}
