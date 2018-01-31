// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: LSStackSlotAllocator.java
package org.graalvm.compiler.lir.stackslotalloc;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class LSStackSlotAllocator_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "LIROptLSStackSlotAllocator": {
            return OptionDescriptor.create(
                /*name*/ "LIROptLSStackSlotAllocator",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use linear scan stack slot allocation.",
                /*declaringClass*/ LSStackSlotAllocator.Options.class,
                /*fieldName*/ "LIROptLSStackSlotAllocator",
                /*option*/ LSStackSlotAllocator.Options.LIROptLSStackSlotAllocator);
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
                    case 0: return get("LIROptLSStackSlotAllocator");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
