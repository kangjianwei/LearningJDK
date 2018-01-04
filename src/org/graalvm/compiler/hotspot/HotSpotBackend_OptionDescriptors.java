// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: HotSpotBackend.java
package org.graalvm.compiler.hotspot;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class HotSpotBackend_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ASMInstructionProfiling": {
            return OptionDescriptor.create(
                /*name*/ "ASMInstructionProfiling",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ String.class,
                /*help*/ "Enables instruction profiling on assembler level. Valid values are a comma separated list of supported instructions. Compare with subclasses of Assembler.InstructionCounter.",
                /*declaringClass*/ HotSpotBackend.Options.class,
                /*fieldName*/ "ASMInstructionProfiling",
                /*option*/ HotSpotBackend.Options.ASMInstructionProfiling);
        }
        case "GraalArithmeticStubs": {
            return OptionDescriptor.create(
                /*name*/ "GraalArithmeticStubs",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use Graal arithmetic stubs instead of HotSpot stubs where possible",
                /*declaringClass*/ HotSpotBackend.Options.class,
                /*fieldName*/ "GraalArithmeticStubs",
                /*option*/ HotSpotBackend.Options.GraalArithmeticStubs);
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
                    case 0: return get("ASMInstructionProfiling");
                    case 1: return get("GraalArithmeticStubs");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
