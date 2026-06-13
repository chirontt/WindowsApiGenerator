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
import net.codecrete.windowsapi.winmd.tables.TypeDef;
import net.codecrete.windowsapi.winmd.tables.TypeRef;

/**
 * Typed, row-level access to the ECMA-335 metadata tables and heaps.
 * <p>
 * This is the seam between reading the binary metadata and consuming it. Consumers
 * ({@link MetadataBuilder} and the decoders) depend on this interface, not on the
 * binary format. The production implementation is {@code WinmdMetadataTables}, backed
 * by a {@link WinmdReader}; tests can supply their own implementation.
 * </p>
 */
interface MetadataSource {

    /**
     * Gets the "ClassLayout" row for the specified parent.
     *
     * @param parent parent (TypeDef index)
     * @return class layout, or {@code null} if none is found
     */
    ClassLayout getClassLayout(int parent);

    /**
     * Gets the "Constant" row for the specified parent.
     *
     * @param parent parent (HasConstant coded index)
     * @return constant
     */
    Constant getConstant(int parent);

    /**
     * Gets the "CustomAttribute" rows for the specified parent.
     *
     * @param parent parent (HasCustomAttribute coded index)
     * @return iterable for iterating the "CustomAttribute" rows
     */
    Iterable<CustomAttribute> getCustomAttributes(int parent);

    /**
     * Gets the "Field" rows for the specified type definition.
     *
     * @param typeDefIndex typeDef (index into TypeDef table)
     * @return iterable for iterating the "Field" rows
     */
    Iterable<Field> getFields(int typeDefIndex);

    /**
     * Gets the "FieldLayout" row for the specified field.
     *
     * @param field field (Field index)
     * @return field layout, or {@code null} if none is found
     */
    FieldLayout getFieldLayout(int field);

    /**
     * Gets the "ImplMap" row for the specified member.
     *
     * @param memberForwarded field or method definition (MemberForwarded coded index)
     * @return implementation map, or {@code null} if none is found
     */
    ImplMap getImplMap(int memberForwarded);

    /**
     * Gets the "InterfaceImpl" rows for the specified class.
     *
     * @param classIndex type definition (index into TypeDef table)
     * @return iterable for iterating the "InterfaceImpl" rows
     */
    Iterable<InterfaceImpl> getInterfaceImpl(int classIndex);

    /**
     * Gets the "MemberRef" row for the specified index.
     *
     * @param index row index
     * @return member reference
     */
    MemberRef getMemberRef(int index);

    /**
     * Gets the "MethodDef" row with the specified index.
     *
     * @param index (MethodDef index)
     * @return method definition entry
     */
    MethodDef getMethodDef(int index);

    /**
     * Gets the "MethodDef" rows for the specified type definition.
     *
     * @param typeDefIndex typeDef (index into TypeDef table)
     * @return iterable for iterating the "MethodDef" rows
     */
    Iterable<MethodDef> getMethodDefs(int typeDefIndex);

    /**
     * Gets the ModuleRef name for the specified index.
     *
     * @param moduleRef (ModuleRef index)
     * @return string index
     */
    int getModuleRefName(int moduleRef);

    /**
     * Gets the "NestedClass" row for the specified class.
     *
     * @param nestedClass nested class (TypeAlias index)
     * @return nested class entry (consisting of nested and enclosing class), or {@code null} if none is found
     */
    NestedClass getNestedClass(int nestedClass);

    /**
     * Gets the "Param" rows for the specified method definition.
     *
     * @param methodDefIndex methodDef (index into MethodDef table)
     * @return iterable for iterating the "Param" rows
     */
    Iterable<Param> getParameters(int methodDefIndex);

    /**
     * Gets the "TypeDef" row for the specified index.
     *
     * @param typeDefIndex typeDef (index into TypeDef table)
     * @return the type definition
     */
    TypeDef getTypeDef(int typeDefIndex);

    /**
     * Gets an iterator over the TypeDef table.
     *
     * @return the iterator
     */
    Iterable<TypeDef> getTypeDefs();

    /**
     * Gets the number of rows in the TypeDef table.
     *
     * @return the number of rows
     */
    int getTypeDefinitionCount();

    /**
     * Gets the "TypeRef" row for the specified index.
     *
     * @param index row index
     * @return type reference
     */
    TypeRef getTypeRef(int index);

    /**
     * Gets the string with the specified index from the 'string' heap.
     *
     * @param index string index
     * @return string
     */
    String getString(int index);

    /**
     * Gets the Blob with the specified index from the 'blob' heap.
     *
     * @param index blob index
     * @return blob handle
     */
    Blob getBlob(int index);
}
