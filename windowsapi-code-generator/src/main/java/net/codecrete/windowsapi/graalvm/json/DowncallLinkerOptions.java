package net.codecrete.windowsapi.graalvm.json;

/**
 * Downcall linker options for GraalVM reachability metadata.
 *
 * @param captureCallState Indicates if the call state (GetLastError()) is captured.
 */
public record DowncallLinkerOptions(
        boolean captureCallState
) {
}
