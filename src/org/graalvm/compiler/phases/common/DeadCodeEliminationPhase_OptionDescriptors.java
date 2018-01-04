// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: DeadCodeEliminationPhase.java
package org.graalvm.compiler.phases.common;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class DeadCodeEliminationPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ReduceDCE": {
            return OptionDescriptor.create(
                /*name*/ "ReduceDCE",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Disable optional dead code eliminations",
                /*declaringClass*/ DeadCodeEliminationPhase.Options.class,
                /*fieldName*/ "ReduceDCE",
                /*option*/ DeadCodeEliminationPhase.Options.ReduceDCE);
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
                    case 0: return get("ReduceDCE");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
