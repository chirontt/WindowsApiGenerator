//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

/**
 * Upcalls signature for GraalVM reachability metadata.
 *
 * @param returnType the return type
 * @param parameterTypes the list of parameters
 */
public record Upcall(
        String returnType,
        List<String> parameterTypes
) {
}
