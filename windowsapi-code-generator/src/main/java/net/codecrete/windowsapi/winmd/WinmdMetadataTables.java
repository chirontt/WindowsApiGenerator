//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.winmd;

import net.codecrete.windowsapi.winmd.tables.ClassLayout;
import net.codecrete.windowsapi.winmd.tables.Constant;
import net.codecrete.windowsapi.winmd.tables.CustomAttribute;
import net.codecrete.windowsapi.winmd.tables.Field;
import net.codecrete.windowsapi.winmd.tables.FieldLayout;
import net.codecrete.windowsapi.winmd.tables.ImplMap;
import net.codecrete.windowsapi.winmd.tables.InterfaceImpl;
import net.codecrete.windowsapi.winmd.tables.MemberRef;
import net.codecrete.windowsapi.winmd.tables.MethodDef;
import net.codecrete.windowsapi.winmd.tables.NestedClass;
import net.codecrete.windowsapi.winmd.tables.Param;
import net.codecrete.windowsapi.winmd.tables.RowKeyTableIterable;
import net.codecrete.windowsapi.winmd.tables.Table;
import net.codecrete.windowsapi.winmd.tables.TableRangeIterable;
import net.codecrete.windowsapi.winmd.tables.TypeDef;
import net.codecrete.windowsapi.winmd.tables.TypeRef;

import java.nio.charset.StandardCharsets;

import static net.codecrete.windowsapi.winmd.tables.CodedIndexes.HAS_CONSTANT_TABLES;
import static net.codecrete.windowsapi.winmd.tables.CodedIndexes.HAS_CUSTOM_ATTRIBUTE_TABLES;
import static net.codecrete.windowsapi.winmd.tables.CodedIndexes.MEMBER_FORWARDED_TABLES;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.CLASS_LAYOUT;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.CONSTANT;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.CUSTOM_ATTRIBUTE;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.FIELD;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.FIELD_LAYOUT;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.IMPL_MAP;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.INTERFACE_IMPL;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.MEMBER_REF;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.METHOD_DEF;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.MODULE_REF;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.NESTED_CLASS;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.PARAM;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.TYPE_DEF;
import static net.codecrete.windowsapi.winmd.tables.MetadataTables.TYPE_REF;

/**
 * Provides typed access to the ECMA-335 metadata tables and heaps.
 * <p>
 * This is the production implementation of {@link MetadataSource}, backed by the
 * {@link RawTables} produced by a {@link WinmdReader}. It interprets the raw table
 * rows into typed records and resolves the string and blob heaps.
 * </p>
 */
class WinmdMetadataTables implements MetadataSource {
    private final Table[] tables;
    private final byte[] stringHeap;
    private final byte[] blobHeap;
    private final Table classLayouts;
    private final Table constants;
    private final Table customAttributes;
    private final Table fields;
    private final Table fieldLayouts;
    private final Table implMaps;
    private final Table interfaceImpls;
    private final Table memberRefs;
    private final Table methodDefs;
    private final Table moduleRefs;
    private final Table nestedClasses;
    private final Table params;
    private final Table typeDefs;
    private final Table typeRefs;
    private final int hasCustomAttributeIndexWidth;
    private final int hasConstantIndexWidth;
    private final int memberForwardedIndexWidth;

    /**
     * Creates a new instance providing typed access to the specified raw tables.
     *
     * @param rawTables the raw tables and heaps
     */
    WinmdMetadataTables(RawTables rawTables) {
        this.tables = rawTables.tables();
        this.stringHeap = rawTables.stringHeap();
        this.blobHeap = rawTables.blobHeap();

        classLayouts = tables[CLASS_LAYOUT];
        constants = tables[CONSTANT];
        customAttributes = tables[CUSTOM_ATTRIBUTE];
        fields = tables[FIELD];
        fieldLayouts = tables[FIELD_LAYOUT];
        implMaps = tables[IMPL_MAP];
        interfaceImpls = tables[INTERFACE_IMPL];
        memberRefs = tables[MEMBER_REF];
        methodDefs = tables[METHOD_DEF];
        moduleRefs = tables[MODULE_REF];
        nestedClasses = tables[NESTED_CLASS];
        params = tables[PARAM];
        typeDefs = tables[TYPE_DEF];
        typeRefs = tables[TYPE_REF];

        // coded index widths needed for primary-key lookups (see ECMA-335, II.24.2.6)
        hasConstantIndexWidth = codedIndexWidth(HAS_CONSTANT_TABLES);
        hasCustomAttributeIndexWidth = codedIndexWidth(HAS_CUSTOM_ATTRIBUTE_TABLES);
        memberForwardedIndexWidth = codedIndexWidth(MEMBER_FORWARDED_TABLES);
    }

    @Override
    public ClassLayout getClassLayout(int parent) {
        var index = classLayouts.indexByPrimaryKey(parent, simpleIndexWidth(TYPE_DEF), 6);
        if (index == 0)
            return null;

        int[] values = new int[3];
        classLayouts.getRow(index, values);
        return new ClassLayout(values[0], values[1], values[2]);
    }

    @Override
    public Constant getConstant(int parent) {
        var index = constants.indexByPrimaryKey(parent, hasConstantIndexWidth, 2);
        assert index != 0;

        int[] values = new int[3];
        constants.getRow(index, values);
        return new Constant(values[0], values[1], values[2]);
    }

    @Override
    public Iterable<CustomAttribute> getCustomAttributes(int parent) {
        return new RowKeyTableIterable<>(customAttributes, parent, hasCustomAttributeIndexWidth, index -> {
            int[] values = new int[3];
            customAttributes.getRow(index, values);
            return new CustomAttribute(values[0], values[1], values[2]);
        });
    }

    @Override
    public Iterable<Field> getFields(int typeDefIndex) {
        int firstField = typeDefs.getValue(typeDefIndex, 4);
        int lastField;
        if (typeDefIndex + 1 <= typeDefs.numRows())
            lastField = typeDefs.getValue(typeDefIndex + 1, 4) - 1;
        else
            lastField = fields.numRows();
        assert firstField <= lastField + 1;

        return new TableRangeIterable<>(firstField, lastField, index -> {
            int[] values = new int[3];
            fields.getRow(index, values);
            return new Field(
                    index,
                    values[0],
                    values[1],
                    values[2]
            );
        });
    }

    @Override
    public FieldLayout getFieldLayout(int field) {
        var index = fieldLayouts.indexByPrimaryKey(field, simpleIndexWidth(FIELD), 4);
        if (index == 0)
            return null;

        int[] values = new int[2];
        fieldLayouts.getRow(index, values);
        return new FieldLayout(values[0], values[1]);
    }

    @Override
    public ImplMap getImplMap(int memberForwarded) {
        var index = implMaps.indexByPrimaryKey(memberForwarded, memberForwardedIndexWidth, 2);
        if (index == 0)
            return null;

        int[] values = new int[4];
        implMaps.getRow(index, values);
        return new ImplMap(values[0], values[1], values[2], values[3]);
    }

    @Override
    public Iterable<InterfaceImpl> getInterfaceImpl(int classIndex) {
        return new RowKeyTableIterable<>(interfaceImpls, classIndex, simpleIndexWidth(TYPE_DEF), index -> {
            int[] values = new int[2];
            interfaceImpls.getRow(index, values);
            return new InterfaceImpl(values[0], values[1]);
        });
    }

    @Override
    public MemberRef getMemberRef(int index) {
        int[] values = new int[3];
        memberRefs.getRow(index, values);
        return new MemberRef(
                values[0],
                values[1],
                values[2]
        );
    }

    @Override
    public MethodDef getMethodDef(int index) {
        int[] values = new int[6];
        methodDefs.getRow(index, values);
        return new MethodDef(
                index,
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5]
        );
    }

    @Override
    public Iterable<MethodDef> getMethodDefs(int typeDefIndex) {
        int firstMethod = typeDefs.getValue(typeDefIndex, 5);
        int lastMethod;
        if (typeDefIndex + 1 <= typeDefs.numRows())
            lastMethod = typeDefs.getValue(typeDefIndex + 1, 5) - 1;
        else
            lastMethod = methodDefs.numRows();
        assert firstMethod <= lastMethod + 1;

        return new TableRangeIterable<>(firstMethod, lastMethod, this::getMethodDef);
    }

    @Override
    public int getModuleRefName(int moduleRef) {
        int[] values = new int[1];
        moduleRefs.getRow(moduleRef, values);
        return values[0];
    }

    @Override
    public NestedClass getNestedClass(int nestedClass) {
        var index = nestedClasses.indexByPrimaryKey(nestedClass, simpleIndexWidth(TYPE_DEF), 0);
        if (index == 0)
            return null;

        int[] values = new int[2];
        nestedClasses.getRow(index, values);
        return new NestedClass(values[0], values[1]);
    }

    @Override
    public Iterable<Param> getParameters(int methodDefIndex) {
        int firstParam = methodDefs.getValue(methodDefIndex, 5);
        int lastParam;
        if (methodDefIndex + 1 <= methodDefs.numRows())
            lastParam = methodDefs.getValue(methodDefIndex + 1, 5) - 1;
        else
            lastParam = params.numRows();
        assert firstParam <= lastParam + 1;

        return new TableRangeIterable<>(firstParam, lastParam, index -> {
            int[] values = new int[3];
            params.getRow(index, values);
            return new Param(
                    index,
                    values[0],
                    values[1],
                    values[2]
            );
        });
    }

    @Override
    public TypeDef getTypeDef(int typeDefIndex) {
        int[] values = new int[6];
        typeDefs.getRow(typeDefIndex, values);
        return new TypeDef(
                values[0],
                values[1],
                values[2],
                values[3],
                values[4],
                values[5]
        );
    }

    @Override
    public Iterable<TypeDef> getTypeDefs() {
        return new TableRangeIterable<>(1, typeDefs.numRows(), this::getTypeDef);
    }

    @Override
    public int getTypeDefinitionCount() {
        return typeDefs.numRows();
    }

    @Override
    public TypeRef getTypeRef(int index) {
        int[] values = new int[3];
        typeRefs.getRow(index, values);
        return new TypeRef(values[0], values[1], values[2]);
    }

    @Override
    public String getString(int index) {
        if (index == 0)
            return null;
        int end = index;
        while (stringHeap[end] != 0)
            end += 1;
        return new String(stringHeap, index, end - index, StandardCharsets.UTF_8);
    }

    @Override
    public Blob getBlob(int index) {
        int b1 = blobHeap[index] & 0xff;
        int length;
        if ((b1 & 0x80) == 0x00) {
            length = b1;
            index += 1;
        } else if ((b1 & 0xc0) == 0x80) {
            length = ((b1 & 0x3f) << 8) + (blobHeap[index + 1] & 0xff);
            index += 2;
        } else if ((b1 & 0xe0) == 0xc0) {
            length = ((b1 & 0x1f) << 24) + ((blobHeap[index + 1] & 0xff) << 16)
                    + ((blobHeap[index + 2] & 0xff) << 8) + (blobHeap[index + 3] & 0xff);
            index += 4;
        } else {
            throw new WinmdException("Invalid data in blob");
        }

        return new Blob(blobHeap, index, length);
    }

    private int simpleIndexWidth(int table) {
        return tables[table] != null ? tables[table].indexWidth() : 2;
    }

    private int codedIndexWidth(int... tableIndexes) {
        int numBitsTable = 32 - Integer.numberOfLeadingZeros(tableIndexes.length - 1);
        int max16BitIndex = 1 << (16 - numBitsTable);
        for (var index : tableIndexes) {
            if (tables[index] != null && tables[index].numRows() >= max16BitIndex)
                return 4;
        }
        return 2;
    }
}
