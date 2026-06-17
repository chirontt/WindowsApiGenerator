package net.codecrete.windowsapi.graalvm.json;

/**
 * Reachability metadata for GraalVM.
 *
 * @param foreign metadata for the Foreign Function and Memory API
 */
public record ReachabilityMetadata(
        ForeignApiConfiguration foreign
) {
}
