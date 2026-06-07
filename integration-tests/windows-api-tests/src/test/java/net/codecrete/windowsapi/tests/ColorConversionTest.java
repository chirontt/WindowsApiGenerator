//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.tests;

import org.junit.jupiter.api.Test;
import windows.win32.graphics.direct2d.D2D1_COLOR_SPACE;
import windows.win32.graphics.direct2d.common.D2D1_COLOR_F;

import java.lang.foreign.Arena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static windows.win32.graphics.direct2d.Apis.D2D1ConvertColorSpace;

class ColorConversionTest {
    @Test
    void queryConsole() {
        try (var arena = Arena.ofConfined()) {
            var sourceColor = D2D1_COLOR_F.allocate(arena);
            D2D1_COLOR_F.r(sourceColor, 1.2f);
            D2D1_COLOR_F.g(sourceColor, -0.1f);
            D2D1_COLOR_F.b(sourceColor, 0.0f);
            D2D1_COLOR_F.a(sourceColor, 1.0f);

            var targetColor = D2D1ConvertColorSpace(
                    arena,
                    D2D1_COLOR_SPACE.SCRGB,
                    D2D1_COLOR_SPACE.SRGB,
                    sourceColor
            );

            assertThat(D2D1_COLOR_F.r(targetColor)).isCloseTo(1.0f, offset(0.01f));
            assertThat(D2D1_COLOR_F.g(targetColor)).isCloseTo(0.0f, offset(0.01f));
            assertThat(D2D1_COLOR_F.b(targetColor)).isCloseTo(0.0f, offset(0.01f));
            assertThat(D2D1_COLOR_F.a(targetColor)).isCloseTo(1.0f, offset(0.01f));
        }

    }
}
