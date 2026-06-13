package net.codecrete.windowsapi.graalvm.json;

import java.util.List;

public record ForeignApiConfiguration(
        List<Downcall> downcalls,
        List<Upcall> upcalls
) {
}
