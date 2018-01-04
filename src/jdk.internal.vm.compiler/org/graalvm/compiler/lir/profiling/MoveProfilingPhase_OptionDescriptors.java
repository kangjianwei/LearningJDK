// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: MoveProfilingPhase.java
package org.graalvm.compiler.lir.profiling;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class MoveProfilingPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIRDynMoveProfileMethod": {
            return OptionDescriptor.create(
                /*name*/ "LIRDynMoveProfileMethod",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable dynamic move profiling per method.",
                /*declaringClass*/ MoveProfilingPhase.Options.class,
                /*fieldName*/ "LIRDynMoveProfileMethod",
                /*option*/ MoveProfilingPhase.Options.LIRDynMoveProfileMethod);
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
                    case 0: return get("LIRDynMoveProfileMethod");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
