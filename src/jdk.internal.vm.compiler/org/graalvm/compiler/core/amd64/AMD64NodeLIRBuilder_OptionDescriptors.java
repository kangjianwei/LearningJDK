// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: AMD64NodeLIRBuilder.java
package org.graalvm.compiler.core.amd64;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class AMD64NodeLIRBuilder_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "MitigateSpeculativeExecutionAttacks": {
            return OptionDescriptor.create(
                /*name*/ "MitigateSpeculativeExecutionAttacks",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Boolean.class,
                /*help*/ "AMD64: Emit lfence instructions at the beginning of basic blocks",
                /*declaringClass*/ AMD64NodeLIRBuilder.Options.class,
                /*fieldName*/ "MitigateSpeculativeExecutionAttacks",
                /*option*/ AMD64NodeLIRBuilder.Options.MitigateSpeculativeExecutionAttacks);
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
                    case 0: return get("MitigateSpeculativeExecutionAttacks");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
