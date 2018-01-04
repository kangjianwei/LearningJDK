// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: BailoutAndRestartBackendException.java
package org.graalvm.compiler.lir;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class BailoutAndRestartBackendException_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIRUnlockBackendRestart": {
            return OptionDescriptor.create(
                /*name*/ "LIRUnlockBackendRestart",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Unlock backend restart feature.",
                /*declaringClass*/ BailoutAndRestartBackendException.Options.class,
                /*fieldName*/ "LIRUnlockBackendRestart",
                /*option*/ BailoutAndRestartBackendException.Options.LIRUnlockBackendRestart);
        }
        // CheckStyle: resume line length check
        }
        return null;
    }

    @Override
    public Iterator<OptionDescriptor> iterator() {
        return new Iterator<OptionDescriptor>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < 1;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("LIRUnlockBackendRestart");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
