//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.winmd;

import net.codecrete.windowsapi.metadata.Member;
import net.codecrete.windowsapi.metadata.Metadata;
import net.codecrete.windowsapi.metadata.Namespace;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.PrimitiveKind;
import net.codecrete.windowsapi.metadata.Struct;
import net.codecrete.windowsapi.metadata.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the struct/union layout calculation on hand-built structs.
 * <p>
 * {@link StructLayouter} is a pure function of the type model, so these tests need
 * neither a .winmd file nor any metadata source.
 * </p>
 */
class StructLayouterTest {

    private final Metadata metadata = new Metadata();
    private final Namespace namespace = metadata.getOrCreateNamespace("Test");
    private int nextTypeDefIndex = 1;

    @Test
    void sequentialStruct_alignsAndPadsFields() {
        var aByte = member("b", primitive(PrimitiveKind.BYTE));
        var anInt = member("i", primitive(PrimitiveKind.INT32));
        var struct = struct(false, 0, aByte, anInt);

        new StructLayouter().layout(struct);

        // the int is aligned to offset 4, so the byte is followed by 3 padding bytes
        assertThat(aByte.offset()).isZero();
        assertThat(aByte.paddingAfter()).isEqualTo(3);
        assertThat(anInt.offset()).isEqualTo(4);
        assertThat(anInt.paddingAfter()).isZero();
        assertThat(struct.structSize()).isEqualTo(8);
        assertThat(struct.packageSize()).isEqualTo(4);
    }

    @Test
    void trailingSmallField_getsTailPadding() {
        var anInt = member("i", primitive(PrimitiveKind.INT32));
        var aByte = member("b", primitive(PrimitiveKind.BYTE));
        var struct = struct(false, 0, anInt, aByte);

        new StructLayouter().layout(struct);

        // the struct is padded up to its 4-byte alignment, so the trailing byte gets 3 padding bytes
        assertThat(anInt.offset()).isZero();
        assertThat(aByte.offset()).isEqualTo(4);
        assertThat(aByte.paddingAfter()).isEqualTo(3);
        assertThat(struct.structSize()).isEqualTo(8);
        assertThat(struct.packageSize()).isEqualTo(4);
    }

    @Test
    void union_isSizedToLargestMember() {
        var anInt = member("i", primitive(PrimitiveKind.INT32));
        var aDouble = member("d", primitive(PrimitiveKind.DOUBLE));
        var union = struct(true, 0, anInt, aDouble);

        new StructLayouter().layout(union);

        // all union members overlay at offset 0; size and alignment follow the largest member
        assertThat(anInt.offset()).isZero();
        assertThat(aDouble.offset()).isZero();
        assertThat(union.structSize()).isEqualTo(8);
        assertThat(union.packageSize()).isEqualTo(8);
    }

    @Test
    void forcedPackageSize_packsFieldsWithoutAlignmentPadding() {
        var aByte = member("b", primitive(PrimitiveKind.BYTE));
        var anInt = member("i", primitive(PrimitiveKind.INT32));
        var struct = struct(false, 1, aByte, anInt);

        new StructLayouter().layout(struct);

        // packed to 1 byte: the int follows the byte immediately, no padding anywhere
        assertThat(aByte.offset()).isZero();
        assertThat(aByte.paddingAfter()).isZero();
        assertThat(anInt.offset()).isEqualTo(1);
        assertThat(anInt.paddingAfter()).isZero();
        assertThat(struct.structSize()).isEqualTo(5);
        assertThat(struct.packageSize()).isEqualTo(1);
    }

    @Test
    void nestedStruct_isLaidOutRecursively() {
        var inner = struct(false, 0, member("i", primitive(PrimitiveKind.INT32)));
        var aByte = member("b", primitive(PrimitiveKind.BYTE));
        var innerMember = member("inner", inner);
        var outer = struct(false, 0, aByte, innerMember);

        new StructLayouter().layout(outer);

        // the inner struct is laid out on demand; it aligns the outer struct to 4 bytes
        assertThat(inner.isLayoutDone()).isTrue();
        assertThat(inner.structSize()).isEqualTo(4);
        assertThat(aByte.offset()).isZero();
        assertThat(aByte.paddingAfter()).isEqualTo(3);
        assertThat(innerMember.offset()).isEqualTo(4);
        assertThat(outer.structSize()).isEqualTo(8);
        assertThat(outer.packageSize()).isEqualTo(4);
    }

    private Primitive primitive(PrimitiveKind kind) {
        return metadata.getPrimitive(kind);
    }

    private Member member(String name, Type type) {
        return new Member(name, 0, type, null);
    }

    private Struct struct(boolean isUnion, int packageSize, Member... members) {
        var struct = new Struct("S" + nextTypeDefIndex, namespace, nextTypeDefIndex, isUnion,
                packageSize, 0, null, null, null);
        nextTypeDefIndex += 1;
        struct.setMembers(List.of(members));
        return struct;
    }
}
