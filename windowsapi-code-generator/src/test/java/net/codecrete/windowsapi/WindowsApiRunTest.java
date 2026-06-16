//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WindowsApiRunTest {

    @Test
    void dryRun_succeeds() {
        var generator = new WindowsApiRun();
        generator.setFunctions(Set.of(
                "WriteFileEx",
                "MessageBoxExW"
        ));
        generator.setStructs(Set.of(
                "SP_DEVINFO_DATA",
                "SP_DEVICE_INTERFACE_DETAIL_DATA_W"
        ));
        generator.setEnumerations(Set.of(
                "WIN32_ERROR"
        ));

        generator.setOutputDirectory(Path.of("target/generated-sources"));

        assertDoesNotThrow(generator::dryRun);
    }

    @Test
    void generateCode_writesReachabilityMetadataFile() throws IOException {
        var temporaryFolder = Files.createTempDirectory("temporary-folder");
        try {
            var metadataFile = temporaryFolder.resolve("reachability-metadata.json");
            var outputDirectory = temporaryFolder.resolve("output");
            Files.createDirectories(outputDirectory);
            var run = new WindowsApiRun();
            run.setFunctions(Set.of("IsWindow"));
            run.setOutputDirectory(outputDirectory);
            run.setReachabilityMetadataFile(metadataFile);

            run.generateCode();

            assertThat(metadataFile).exists();
            assertThat(Files.readString(metadataFile)).contains("\"downcalls\"").contains("void*");
        } finally {
            Testing.deleteDirectory(temporaryFolder);
        }
    }

    @Test
    void dryRun_doesNotWriteReachabilityMetadataFile() throws IOException {
        var temporaryFolder = Files.createTempDirectory("temporary-folder");
        try {
            var metadataFile = temporaryFolder.resolve("reachability-metadata.json");
            var run = new WindowsApiRun();
            run.setFunctions(Set.of("IsWindow"));
            run.setOutputDirectory(temporaryFolder.resolve("output"));
            run.setReachabilityMetadataFile(metadataFile);

            run.dryRun();

            assertThat(metadataFile).doesNotExist();
        } finally {
            Testing.deleteDirectory(temporaryFolder);
        }
    }

    @Test
    void createDirectory_succeeds() throws IOException {
        var temporaryFolder = Files.createTempDirectory("temporary-folder");
        try {
            var outputDirectory = temporaryFolder.resolve("output");
            var run = new WindowsApiRun();
            run.createDirectory(outputDirectory);
            assertThat(outputDirectory).exists();

        } finally {
            Testing.deleteDirectory(temporaryFolder);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void cleanOldFiles_succeeds() throws IOException {
        var temporaryFolder = Files.createTempDirectory("temporary-folder");
        try {
            var outputDirectory = temporaryFolder.resolve("output");
            var inner1Directory = outputDirectory.resolve("inner1");
            var inner2Directory = inner1Directory.resolve("inner2");
            inner2Directory.toFile().mkdirs();
            var inner3Directory = inner1Directory.resolve("inner3");
            inner3Directory.toFile().mkdirs();
            var inner2JavaPath = inner2Directory.resolve("inner2.java");
            Files.writeString(inner2JavaPath, "Hello World");
            Files.writeString(inner2Directory.resolve("inner3.java"), "Hello World");

            var relativeJavaPath = outputDirectory.relativize(inner2JavaPath);

            var run = new WindowsApiRun();
            run.setOutputDirectory(outputDirectory);
            run.deleteOldFiles(Set.of(relativeJavaPath));

            assertThat(inner2Directory).exists();
            assertThat(inner3Directory).doesNotExist();

        } finally {
            Testing.deleteDirectory(temporaryFolder);
        }
    }
}
