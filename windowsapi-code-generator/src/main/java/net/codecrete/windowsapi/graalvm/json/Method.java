//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

/**
 * Method descriptor for reflection.
 * @param name the method name
 * @param parameterTypes the method's parameter types (Java types)
 */
public record Method(String name, List<String> parameterTypes) {
}
