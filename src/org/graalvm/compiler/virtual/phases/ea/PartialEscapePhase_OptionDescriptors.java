// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: PartialEscapePhase.java
package org.graalvm.compiler.virtual.phases.ea;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class PartialEscapePhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "OptEarlyReadElimination": {
            return OptionDescriptor.create(
                /*name*/ "OptEarlyReadElimination",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ PartialEscapePhase.Options.class,
                /*fieldName*/ "OptEarlyReadElimination",
                /*option*/ PartialEscapePhase.Options.OptEarlyReadElimination);
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
                    case 0: return get("OptEarlyReadElimination");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
