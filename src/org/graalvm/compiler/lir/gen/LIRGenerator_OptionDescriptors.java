// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: LIRGenerator.java
package org.graalvm.compiler.lir.gen;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class LIRGenerator_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "PrintIRWithLIR": {
            return OptionDescriptor.create(
                /*name*/ "PrintIRWithLIR",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Print HIR along side LIR as the latter is generated",
                /*declaringClass*/ LIRGenerator.Options.class,
                /*fieldName*/ "PrintIRWithLIR",
                /*option*/ LIRGenerator.Options.PrintIRWithLIR);
        }
        case "TraceLIRGeneratorLevel": {
            return OptionDescriptor.create(
                /*name*/ "TraceLIRGeneratorLevel",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "The trace level for the LIR generator",
                /*declaringClass*/ LIRGenerator.Options.class,
                /*fieldName*/ "TraceLIRGeneratorLevel",
                /*option*/ LIRGenerator.Options.TraceLIRGeneratorLevel);
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
                return i < 2;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("PrintIRWithLIR");
                    case 1: return get("TraceLIRGeneratorLevel");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
