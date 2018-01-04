// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilationStatistics.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilationStatistics_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "UseCompilationStatistics": {
            return OptionDescriptor.create(
                /*name*/ "UseCompilationStatistics",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enables CompilationStatistics.",
                /*declaringClass*/ CompilationStatistics.Options.class,
                /*fieldName*/ "UseCompilationStatistics",
                /*option*/ CompilationStatistics.Options.UseCompilationStatistics);
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
                    case 0: return get("UseCompilationStatistics");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
