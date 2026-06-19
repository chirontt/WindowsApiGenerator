//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

/**
 * Reachability metadata for GraalVM.
 *
 * @param foreign metadata for the Foreign Function and Memory API
 * @param reflection metadata for elements that should be accessible by reflection
 */
public record ReachabilityMetadata(
        ForeignApiConfiguration foreign,
        List<ReflectionObject> reflection
) {
}
