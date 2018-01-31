// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: JavaConstantFieldProvider.java
package org.graalvm.compiler.core.common.spi;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class JavaConstantFieldProvider_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "TrustFinalDefaultFields": {
            return OptionDescriptor.create(
                /*name*/ "TrustFinalDefaultFields",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Determines whether to treat final fields with default values as constant.",
                /*declaringClass*/ JavaConstantFieldProvider.Options.class,
                /*fieldName*/ "TrustFinalDefaultFields",
                /*option*/ JavaConstantFieldProvider.Options.TrustFinalDefaultFields);
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
                    case 0: return get("TrustFinalDefaultFields");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
