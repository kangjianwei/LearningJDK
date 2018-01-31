// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: TraceBuilderPhase.java
package org.graalvm.compiler.lir.alloc.trace;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class TraceBuilderPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "TraceBuilding": {
            return OptionDescriptor.create(
                /*name*/ "TraceBuilding",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ org.graalvm.compiler.lir.alloc.trace.TraceBuilderPhase.TraceBuilder.class,
                /*help*/ "Trace building algorithm.",
                /*declaringClass*/ TraceBuilderPhase.Options.class,
                /*fieldName*/ "TraceBuilding",
                /*option*/ TraceBuilderPhase.Options.TraceBuilding);
        }
        case "TraceRAScheduleTrivialTracesEarly": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAScheduleTrivialTracesEarly",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Schedule trivial traces as early as possible.",
                /*declaringClass*/ TraceBuilderPhase.Options.class,
                /*fieldName*/ "TraceRAScheduleTrivialTracesEarly",
                /*option*/ TraceBuilderPhase.Options.TraceRAScheduleTrivialTracesEarly);
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
                    case 0: return get("TraceBuilding");
                    case 1: return get("TraceRAScheduleTrivialTracesEarly");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
