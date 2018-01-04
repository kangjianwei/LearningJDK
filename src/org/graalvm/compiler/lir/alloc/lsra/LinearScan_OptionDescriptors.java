// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: LinearScan.java
package org.graalvm.compiler.lir.alloc.lsra;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class LinearScan_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptLSRAOptimizeSpillPosition": {
            return OptionDescriptor.create(
                /*name*/ "LIROptLSRAOptimizeSpillPosition",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable spill position optimization",
                /*declaringClass*/ LinearScan.Options.class,
                /*fieldName*/ "LIROptLSRAOptimizeSpillPosition",
                /*option*/ LinearScan.Options.LIROptLSRAOptimizeSpillPosition);
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
                    case 0: return get("LIROptLSRAOptimizeSpillPosition");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
