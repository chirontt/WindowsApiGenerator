//
// Windows API Generator for Java
// Copyright (c) 2026 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.tests;

import org.assertj.core.api.AbstractAssert;

import java.lang.foreign.MemorySegment;

import static windows.win32.foundation.WIN32_ERROR.ERROR_SUCCESS;

public class WindowsCallStateAssert extends AbstractAssert<WindowsCallStateAssert, MemorySegment> {

    protected WindowsCallStateAssert(MemorySegment actual) {
        super(actual, WindowsCallStateAssert.class);
    }

    public static WindowsCallStateAssert assertThat(MemorySegment actual) {
        return new WindowsCallStateAssert(actual);
    }

    public WindowsCallStateAssert isSuccessful() {
        var lastError = Windows.getLastError(actual);
        if (lastError != ERROR_SUCCESS) {
            failWithMessage("Expected success but failed (%s)",
                    Windows.getErrorMessage(lastError));
        }

        return this;
    }
}
