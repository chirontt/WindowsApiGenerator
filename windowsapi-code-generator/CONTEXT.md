# Windows API Code Generator

Domain language for the library that reads the Windows API metadata and generates
Java source code for it. The pipeline is: read metadata → build a type model →
select a scope → generate Java source.

## Language

### Reading metadata

**winmd**:
The binary `Windows.Win32.winmd` file: Windows API metadata in PE / ECMA-335 form.
_Avoid_: assembly, DLL.

**WinmdReader**:
The component that parses the **winmd** binary (PE headers, streams, heaps) and
produces the raw ECMA-335 tables. Knows the file format; knows nothing about types.

**MetadataSource**:
The seam giving typed, row-level access to the ECMA-335 metadata tables and the
string/blob heaps (`getTypeDef`, `getFields`, `getBlob`, …). Format-agnostic: its
consumers depend on this, not on **WinmdReader**. The production adapter is
`WinmdMetadataTables` (backed by **WinmdReader**'s output); tests supply fakes.
_Avoid_: MetadataFile, MetadataTables (the latter names the table-id constants), MetadataReader.

**RawTables**:
The immutable hand-off between **WinmdReader** and `WinmdMetadataTables`: the raw
`Table[]` plus the string and blob heaps.

### The type model

**Metadata**:
The fully built, in-memory model of the Windows API — namespaces, types, methods,
constants — produced by **MetadataBuilder** from a **MetadataSource**. The result the
rest of the pipeline consumes.

**MetadataBuilder**:
Drives the multi-stage construction of **Metadata** from a **MetadataSource**.

**Scope**:
The user-selected set of types, functions, and constants to generate, plus its
transitive closure (a selected struct pulls in the types it references).

### Generating code

**SourceFileSink**:
The seam between deciding what Java source to emit and where the bytes go
(disk / discarded / captured in memory).

## Relationships

- **WinmdReader** parses one **winmd** file and produces **RawTables**
- `WinmdMetadataTables` adapts **RawTables** to the **MetadataSource** seam
- **MetadataBuilder** reads a **MetadataSource** and produces **Metadata**
- A **Scope** selects from **Metadata**; the code writers emit it through a **SourceFileSink**

## Example dialogue

> **Dev:** "Does `StructLayouter` need the **winmd** file to compute field offsets?"
> **Author:** "No — it only needs field-layout rows. Give it a **MetadataSource** and it
> works against a fake just as well as against a real **WinmdReader**."

## Flagged ambiguities

- "MetadataTables" already names the table-id constants in `winmd.tables`; the typed
  access seam is **MetadataSource**, not `MetadataTables`.
