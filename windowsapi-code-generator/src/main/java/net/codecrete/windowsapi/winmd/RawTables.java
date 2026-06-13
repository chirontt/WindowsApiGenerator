//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.winmd;

import net.codecrete.windowsapi.winmd.tables.Table;

/**
 * Raw ECMA-335 metadata tables and heaps, as produced by {@link WinmdReader}.
 * <p>
 * This is the immutable hand-off between reading the binary metadata and providing
 * typed access to it ({@code WinmdMetadataTables}). The tables have their column widths
 * set, so rows can be read, but no row has been interpreted into a typed record yet.
 * </p>
 *
 * @param tables     the metadata tables, indexed by table id (see {@code MetadataTables})
 * @param stringHeap the '#Strings' heap
 * @param blobHeap   the '#Blob' heap
 */
@SuppressWarnings("java:S6218")
record RawTables(Table[] tables, byte[] stringHeap, byte[] blobHeap) {
}
