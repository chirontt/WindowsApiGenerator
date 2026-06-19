//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.graalvm;

import net.codecrete.windowsapi.SimpleEventListener;
import net.codecrete.windowsapi.graalvm.json.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReachabilityMetadataWriterTest {

    private String write(ReachabilityMetadata metadata) throws IOException {
        var writer = new StringWriter();
        new ReachabilityMetadataWriter(new SimpleEventListener()).write(metadata, writer);
        // Normalize platform line separators (the JSON writer emits the platform separator,
        // e.g. CRLF on Windows) so the expectations below can use plain LF text blocks.
        return writer.toString().replace("\r\n", "\n");
    }

    @Test
    void write_downcalls_emitsLinkerOptionsOnlyWhenCapturing() throws IOException {
        var metadata = new ReachabilityMetadata(new ForeignApiConfiguration(
                List.of(
                        new Downcall("void", List.of(), new DowncallLinkerOptions(false)),
                        new Downcall("jint", List.of("void*", "struct(jchar, padding(7), void*)"),
                                new DowncallLinkerOptions(true))
                ),
                List.of()),
                null);

        assertThat(write(metadata)).isEqualTo("""
                {
                    "foreign": {
                        "downcalls": [
                            {
                                "returnType": "void",
                                "parameterTypes": [
                                ]
                            },
                            {
                                "returnType": "jint",
                                "parameterTypes": [
                                    "void*",
                                    "struct(jchar, padding(7), void*)"
                                ],
                                "options": {
                                    "captureCallState": true
                                }
                            }
                        ]
                    }
                }
                """);
    }

    @Test
    void write_reflection_emitsTypesAndMethods() throws IOException {
        var metadata = new ReachabilityMetadata(
                new ForeignApiConfiguration(List.of(), List.of()),
                List.of(new ReflectionObject(
                        "windows.win32.ui.windowsandmessaging.WNDPROC$Function",
                        List.of(new Method("invoke",
                                List.of("java.lang.foreign.MemorySegment", "int", "long", "long"))))));

        assertThat(write(metadata)).isEqualTo("""
                {
                    "foreign": {
                    },
                    "reflection": [
                        {
                            "type": "windows.win32.ui.windowsandmessaging.WNDPROC$Function",
                            "methods": [
                                {
                                    "name": "invoke",
                                    "parameterTypes": [
                                        "java.lang.foreign.MemorySegment",
                                        "int",
                                        "long",
                                        "long"
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """);
    }

    @Test
    void write_emptyReflection_omitsReflectionKey() throws IOException {
        var metadata = new ReachabilityMetadata(
                new ForeignApiConfiguration(List.of(), List.of()), List.of());

        assertThat(write(metadata)).isEqualTo("""
                {
                    "foreign": {
                    }
                }
                """);
    }

    @Test
    void write_upcalls_haveNoLinkerOptions() throws IOException {
        var metadata = new ReachabilityMetadata(new ForeignApiConfiguration(
                List.of(),
                List.of(new Upcall("jlong", List.of("void*", "jint")))),
                null);

        assertThat(write(metadata)).isEqualTo("""
                {
                    "foreign": {
                        "upcalls": [
                            {
                                "returnType": "jlong",
                                "parameterTypes": [
                                    "void*",
                                    "jint"
                                ]
                            }
                        ]
                    }
                }
                """);
    }
}
