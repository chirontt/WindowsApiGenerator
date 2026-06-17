package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

/**
 * Foreign Function And Memory API reachability metadata for GraalVM.
 *
 * @param downcalls the downcall signatures
 * @param upcalls the upcall signatures
 */
public record ForeignApiConfiguration(
        List<Downcall> downcalls,
        List<Upcall> upcalls
) {
}
