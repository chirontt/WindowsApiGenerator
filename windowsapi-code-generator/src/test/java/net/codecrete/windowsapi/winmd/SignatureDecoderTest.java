//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.winmd;

import net.codecrete.windowsapi.metadata.Array;
import net.codecrete.windowsapi.metadata.Metadata;
import net.codecrete.windowsapi.metadata.Namespace;
import net.codecrete.windowsapi.metadata.Pointer;
import net.codecrete.windowsapi.metadata.Primitive;
import net.codecrete.windowsapi.metadata.PrimitiveKind;
import net.codecrete.windowsapi.metadata.Struct;
import net.codecrete.windowsapi.metadata.Type;
import net.codecrete.windowsapi.winmd.tables.CodedIndex;
import net.codecrete.windowsapi.winmd.tables.CodedIndexes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static net.codecrete.windowsapi.winmd.tables.MetadataTables.TYPE_DEF;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.TYPE_REF;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the ECMA-335 signature decoding on hand-built blobs.
 * <p>
 * {@link SignatureDecoder} resolves types through the {@link TypeLookup} seam, so these
 * tests drive it with a fake lookup and synthetic signature blobs &mdash; no .winmd file.
 * </p>
 */
class SignatureDecoderTest {

    private final FakeTypeLookup typeLookup = new FakeTypeLookup();
    private final SignatureDecoder decoder = new SignatureDecoder(typeLookup);

    @Test
    void fieldSignature_decodesPrimitive() {
        var type = decoder.decodeFieldSignature(fieldSignature(ElementTypes.I4), null, null);

        assertThat(type).isEqualTo(typeLookup.primitive(PrimitiveKind.INT32));
    }

    @Test
    void fieldSignature_decodesPointerToPrimitive() {
        var type = decoder.decodeFieldSignature(fieldSignature(ElementTypes.PTR, ElementTypes.I4), null, null);

        assertThat(type).isInstanceOf(Pointer.class);
        assertThat(((Pointer) type).referencedType()).isEqualTo(typeLookup.primitive(PrimitiveKind.INT32));
    }

    @Test
    void fieldSignature_decodesFixedLengthArray() {
        // ARRAY <itemType> rank=1 numSizes=1 size=4 numLoBounds=0
        var type = decoder.decodeFieldSignature(
                fieldSignature(ElementTypes.ARRAY, ElementTypes.I4, 1, 1, 4, 0), null, null);

        assertThat(type).isInstanceOf(Array.class);
        var array = (Array) type;
        assertThat(array.itemType()).isEqualTo(typeLookup.primitive(PrimitiveKind.INT32));
        assertThat(array.arrayLength()).isEqualTo(4);
    }

    @Test
    void fieldSignature_decodesValueType_viaTypeDef() {
        var struct = typeLookup.stubTypeDef(7, "MyStruct");

        var type = decoder.decodeFieldSignature(
                fieldSignature(ElementTypes.VALUETYPE, typeDefOrRef(TYPE_DEF, 7)), null, null);

        assertThat(type).isSameAs(struct);
    }

    @Test
    void fieldSignature_decodesClass_viaTypeRef_passingParentAndNamespace() {
        var referenced = typeLookup.stubTypeRef(3, "MyClass");
        var parent = new Struct("Parent", typeLookup.namespace(), 99, false, 0, 0, null, null, null);

        var type = decoder.decodeFieldSignature(
                fieldSignature(ElementTypes.CLASS, typeDefOrRef(TYPE_REF, 3)), parent, "My.Namespace");

        assertThat(type).isSameAs(referenced);
        // a CLASS reference may resolve to a type from another assembly
        assertThat(typeLookup.lastExternalTypeAllowed).isTrue();
        assertThat(typeLookup.lastParentType).isSameAs(parent);
        assertThat(typeLookup.lastNamespace).isEqualTo("My.Namespace");
    }

    @Test
    void methodSignature_decodesParameterlessVoidMethod() {
        // flags=DEFAULT paramCount=0 returnType=VOID
        var signature = decoder.decodeMethodDefSignature(blob(0x00, 0x00, ElementTypes.VOID));

        assertThat(signature.returnType()).isEqualTo(typeLookup.primitive(PrimitiveKind.VOID));
        assertThat(signature.paramTypes()).isEmpty();
    }

    @Test
    void methodSignature_decodesReturnTypeAndParameters() {
        // flags=DEFAULT paramCount=2 returnType=I4 param0=I4 param1=PTR I4
        var signature = decoder.decodeMethodDefSignature(
                blob(0x00, 0x02, ElementTypes.I4, ElementTypes.I4, ElementTypes.PTR, ElementTypes.I4));

        var int32 = typeLookup.primitive(PrimitiveKind.INT32);
        assertThat(signature.returnType()).isEqualTo(int32);
        assertThat(signature.paramTypes()).hasSize(2);
        assertThat(signature.paramTypes()[0]).isEqualTo(int32);
        assertThat(signature.paramTypes()[1]).isInstanceOf(Pointer.class);
        assertThat(((Pointer) signature.paramTypes()[1]).referencedType()).isEqualTo(int32);
    }

    /**
     * Builds a field signature blob: the {@code FIELD} prolog (0x06) followed by a type.
     */
    private static Blob fieldSignature(int... typeBytes) {
        var bytes = new int[typeBytes.length + 1];
        bytes[0] = 0x06; // FIELD calling convention (ECMA-335, II.23.2.4)
        System.arraycopy(typeBytes, 0, bytes, 1, typeBytes.length);
        return blob(bytes);
    }

    private static Blob blob(int... bytes) {
        var data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            data[i] = (byte) bytes[i];
        return new Blob(data, 0, data.length);
    }

    /**
     * Encodes a {@code TypeDefOrRef} coded index as a single compressed byte.
     */
    private static int typeDefOrRef(int table, int index) {
        var encoded = CodedIndex.encode(table, index, CodedIndexes.TYPE_DEF_OR_REF_TABLES);
        assert encoded <= 0x7f : "coded index does not fit in a single compressed byte";
        return encoded;
    }

    /**
     * A {@link TypeLookup} backed by a real {@link Metadata} for primitives and pointers,
     * with hand-registered stubs for TypeDef and TypeRef resolution.
     */
    private static final class FakeTypeLookup implements TypeLookup {
        private final Metadata metadata = new Metadata();
        private final Namespace namespace = metadata.getOrCreateNamespace("Test");
        private final Map<Integer, Primitive> primitives = new HashMap<>();
        private final Map<Integer, Type> typeDefs = new HashMap<>();
        private final Map<Integer, Type> typeRefs = new HashMap<>();

        private Struct lastParentType;
        private String lastNamespace;
        private boolean lastExternalTypeAllowed;

        FakeTypeLookup() {
            primitives.put(ElementTypes.VOID, metadata.getPrimitive(PrimitiveKind.VOID));
            primitives.put(ElementTypes.I4, metadata.getPrimitive(PrimitiveKind.INT32));
            primitives.put(ElementTypes.U1, metadata.getPrimitive(PrimitiveKind.BYTE));
        }

        Primitive primitive(PrimitiveKind kind) {
            return metadata.getPrimitive(kind);
        }

        Namespace namespace() {
            return namespace;
        }

        Struct stubTypeDef(int index, String name) {
            var struct = new Struct(name, namespace, index, false, 0, 0, null, null, null);
            typeDefs.put(index, struct);
            return struct;
        }

        Struct stubTypeRef(int index, String name) {
            var struct = new Struct(name, namespace, 1000 + index, false, 0, 0, null, null, null);
            typeRefs.put(index, struct);
            return struct;
        }

        @Override
        public Primitive getPrimitiveType(int elementType) {
            return primitives.get(elementType);
        }

        @Override
        public Type getTypeByTypeDef(int typeDefIndex) {
            return typeDefs.get(typeDefIndex);
        }

        @Override
        public Type getTypeByTypeRef(int typeRefIndex, Struct parentType, String currentNamespace,
                                     boolean externalTypeAllowed) {
            lastParentType = parentType;
            lastNamespace = currentNamespace;
            lastExternalTypeAllowed = externalTypeAllowed;
            return typeRefs.get(typeRefIndex);
        }

        @Override
        public int getElementType(Primitive primitiveType) {
            throw new UnsupportedOperationException("not used by signature decoding");
        }

        @Override
        public Pointer makePointerFor(Type type) {
            return metadata.makePointerFor(type);
        }
    }
}
