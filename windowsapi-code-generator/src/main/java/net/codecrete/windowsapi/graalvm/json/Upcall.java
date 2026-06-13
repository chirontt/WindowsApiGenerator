package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

public record Upcall(
        String returnType,
        List<String> parameterTypes
) {
}
