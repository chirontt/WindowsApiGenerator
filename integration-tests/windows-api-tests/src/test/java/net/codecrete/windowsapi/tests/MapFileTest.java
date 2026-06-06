package net.codecrete.windowsapi.tests;

import org.junit.jupiter.api.Test;
import windows.win32.system.memory.FILE_MAP;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static org.assertj.core.api.Assertions.assertThat;
import static windows.win32.foundation.Apis.CloseHandle;
import static windows.win32.foundation.GENERIC_ACCESS_RIGHTS.GENERIC_READ;
import static windows.win32.foundation.GENERIC_ACCESS_RIGHTS.GENERIC_WRITE;
import static windows.win32.storage.filesystem.Apis.CreateFileW;
import static windows.win32.storage.filesystem.FILE_CREATION_DISPOSITION.OPEN_EXISTING;
import static windows.win32.storage.filesystem.FILE_FLAGS_AND_ATTRIBUTES.FILE_ATTRIBUTE_NORMAL;
import static windows.win32.system.memory.Apis.CreateFileMappingW;
import static windows.win32.system.memory.Apis.MapViewOfFile;
import static windows.win32.system.memory.Apis.UnmapViewOfFile;
import static windows.win32.system.memory.PAGE_PROTECTION_FLAGS.PAGE_READWRITE;

class MapFileTest {

    @Test
    void mapFileIntoMemory() throws IOException {
        Files.writeString(Paths.get("sample_file.txt"), "Hello, world!");

        var errorStateLayout = Linker.Option.captureStateLayout();

        try (var arena = Arena.ofConfined()) {
            var errorState = arena.allocate(errorStateLayout);

            var fileHandle = CreateFileW(
                    errorState,
                    arena.allocateFrom("sample_file.txt", UTF_16LE),
                    GENERIC_READ | GENERIC_WRITE,
                    0,
                    NULL,
                    OPEN_EXISTING,
                    FILE_ATTRIBUTE_NORMAL,
                    NULL
            );
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();

            var mappingHandle = CreateFileMappingW(
                    errorState,
                    fileHandle,
                    NULL,
                    PAGE_READWRITE,
                    0, 0, // High and low size (0 means use file's actual size)
                    NULL
            );
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();

            var buf = MapViewOfFile(
                    errorState,
                    mappingHandle,
                    FILE_MAP.ALL_ACCESS,
                    0, 0, // Offset 0 (beginning of file)
                    0 // 0 means map the entire file
            );
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();

            buf = buf.reinterpret(1000);

            var firstChar = buf.get(JAVA_BYTE, 0);
            assertThat(firstChar).isEqualTo((byte)'H');

            UnmapViewOfFile(errorState, buf);
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();

            CloseHandle(errorState, mappingHandle);
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();

            CloseHandle(errorState, fileHandle);
            WindowsCallStateAssert.assertThat(errorState).isSuccessful();
        }

    }
}
