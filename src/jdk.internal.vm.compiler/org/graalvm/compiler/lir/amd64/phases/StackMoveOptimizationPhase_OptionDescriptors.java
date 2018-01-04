// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: StackMoveOptimizationPhase.java
package org.graalvm.compiler.lir.amd64.phases;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class StackMoveOptimizationPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptStackMoveOptimizer": {
            return OptionDescriptor.create(
                /*name*/ "LIROptStackMoveOptimizer",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "",
                /*declaringClass*/ StackMoveOptimizationPhase.Options.class,
                /*fieldName*/ "LIROptStackMoveOptimizer",
                /*option*/ StackMoveOptimizationPhase.Options.LIROptStackMoveOptimizer);
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
                    case 0: return get("LIROptStackMoveOptimizer");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
