// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: CompilationCounters.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class CompilationCounters_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "CompilationCountLimit": {
            return OptionDescriptor.create(
                /*name*/ "CompilationCountLimit",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "The number of compilations allowed for any method before the VM exits (a value of 0 means there is no limit).",
                /*declaringClass*/ CompilationCounters.Options.class,
                /*fieldName*/ "CompilationCountLimit",
                /*option*/ CompilationCounters.Options.CompilationCountLimit);
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
                    case 0: return get("CompilationCountLimit");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
