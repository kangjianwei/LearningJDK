// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: ConstantLoadOptimization.java
package org.graalvm.compiler.lir.constopt;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class ConstantLoadOptimization_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptConstantLoadOptimization": {
            return OptionDescriptor.create(
                /*name*/ "LIROptConstantLoadOptimization",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Enable constant load optimization.",
                /*declaringClass*/ ConstantLoadOptimization.Options.class,
                /*fieldName*/ "LIROptConstantLoadOptimization",
                /*option*/ ConstantLoadOptimization.Options.LIROptConstantLoadOptimization);
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
                    case 0: return get("LIROptConstantLoadOptimization");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
