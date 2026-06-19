//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

/**
 * Object accessible via reflection
 * @param type the object type (full Java class name)
 * @param methods the methods that should be available
 */
public record ReflectionObject(String type, List<Method> methods) {
}
