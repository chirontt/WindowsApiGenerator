package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

public record Downcall(
        String returnType,
        List<String> parameterTypes,
        DowncallLinkerOptions linkerOptions
) {
}
