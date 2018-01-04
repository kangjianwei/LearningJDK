// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: UseTrappingNullChecksPhase.java
package org.graalvm.compiler.phases.common;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class UseTrappingNullChecksPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "UseTrappingNullChecks": {
            return OptionDescriptor.create(
                /*name*/ "UseTrappingNullChecks",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use traps for null checks instead of explicit null-checks",
                /*declaringClass*/ UseTrappingNullChecksPhase.Options.class,
                /*fieldName*/ "UseTrappingNullChecks",
                /*option*/ UseTrappingNullChecksPhase.Options.UseTrappingNullChecks);
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
                    case 0: return get("UseTrappingNullChecks");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
