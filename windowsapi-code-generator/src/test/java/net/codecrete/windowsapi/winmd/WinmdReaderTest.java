//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.winmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class WinmdReaderTest {
    WinmdReader reader;

    @BeforeEach
    void setUp() throws IOException {
        try (var stream = MetadataBuilder.class.getClassLoader().getResourceAsStream("Windows.Win32.winmd")) {
            reader = new WinmdReader(stream);
        }
    }

    @Test
    void readHeader() {
        assertThat(reader.getVersion()).isEqualTo("v4.0.30319");

        assertThat(reader.getStreams()).extracting(WinmdReader.MetadataStream::name)
                .containsExactlyInAnyOrder("#GUID", "#~", "#US", "#Strings", "#Blob");

        assertThat(reader.getStreams()).extracting(WinmdReader.MetadataStream::offset).isSorted();
    }

    @Test
    void readTypeDefinitions() {
        var source = new WinmdMetadataTables(reader.rawTables());
        int count = 0;
        for (var ignored : source.getTypeDefs()) {
            count += 1;
        }
        assertThat(count).isGreaterThan(10000);
    }
}
