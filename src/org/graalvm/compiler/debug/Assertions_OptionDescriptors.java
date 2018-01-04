// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: Assertions.java
package org.graalvm.compiler.debug;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class Assertions_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "DetailedAsserts": {
            return OptionDescriptor.create(
                /*name*/ "DetailedAsserts",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable expensive assertions if normal assertions (i.e. -ea or -esa) are enabled.",
                /*declaringClass*/ Assertions.Options.class,
                /*fieldName*/ "DetailedAsserts",
                /*option*/ Assertions.Options.DetailedAsserts);
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
                    case 0: return get("DetailedAsserts");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
