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
        return writer.toString();
    }

    @Test
    void write_emptyConfiguration_producesEmptyArrays() throws IOException {
        var metadata = new ReachabilityMetadata(new ForeignApiConfiguration(List.of(), List.of()));

        assertThat(write(metadata)).isEqualTo("""
                {
                  "foreign": {
                    "downcalls": [],
                    "upcalls": []
                  }
                }
                """);
    }

    @Test
    void write_downcalls_emitsLinkerOptionsOnlyWhenCapturing() throws IOException {
        var metadata = new ReachabilityMetadata(new ForeignApiConfiguration(
                List.of(
                        new Downcall("void", List.of(), new DowncallLinkerOptions(false)),
                        new Downcall("jint", List.of("void*", "struct(jchar, padding(7), void*)"),
                                new DowncallLinkerOptions(true))
                ),
                List.of()));

        assertThat(write(metadata)).isEqualTo("""
                {
                  "foreign": {
                    "downcalls": [
                      {
                        "returnType": "void",
                        "parameterTypes": []
                      },
                      {
                        "returnType": "jint",
                        "parameterTypes": ["void*", "struct(jchar, padding(7), void*)"],
                        "linkerOptions": {
                          "captureCallState": true
                        }
                      }
                    ],
                    "upcalls": []
                  }
                }
                """);
    }

    @Test
    void write_upcalls_haveNoLinkerOptions() throws IOException {
        var metadata = new ReachabilityMetadata(new ForeignApiConfiguration(
                List.of(),
                List.of(new Upcall("jlong", List.of("void*", "jint")))));

        assertThat(write(metadata)).isEqualTo("""
                {
                  "foreign": {
                    "downcalls": [],
                    "upcalls": [
                      {
                        "returnType": "jlong",
                        "parameterTypes": ["void*", "jint"]
                      }
                    ]
                  }
                }
                """);
    }
}
