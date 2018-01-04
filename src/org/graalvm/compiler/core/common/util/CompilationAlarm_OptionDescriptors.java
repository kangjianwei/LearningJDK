// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilationAlarm.java
package org.graalvm.compiler.core.common.util;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilationAlarm_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "CompilationExpirationPeriod": {
            return OptionDescriptor.create(
                /*name*/ "CompilationExpirationPeriod",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Time limit in seconds before a compilation expires (0 to disable the limit). The compilation alarm will be implicitly disabled if assertions are enabled.",
                /*declaringClass*/ CompilationAlarm.Options.class,
                /*fieldName*/ "CompilationExpirationPeriod",
                /*option*/ CompilationAlarm.Options.CompilationExpirationPeriod);
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
                    case 0: return get("CompilationExpirationPeriod");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
