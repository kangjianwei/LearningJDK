// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: TraceRegisterAllocationPhase.java
package org.graalvm.compiler.lir.alloc.trace;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class TraceRegisterAllocationPhase_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "TraceRACacheStackSlots": {
            return OptionDescriptor.create(
                /*name*/ "TraceRACacheStackSlots",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Cache stack slots globally (i.e. a variable always gets the same slot in every trace).",
                /*declaringClass*/ TraceRegisterAllocationPhase.Options.class,
                /*fieldName*/ "TraceRACacheStackSlots",
                /*option*/ TraceRegisterAllocationPhase.Options.TraceRACacheStackSlots);
        }
        case "TraceRAreuseStackSlotsForMoveResolutionCycleBreaking": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAreuseStackSlotsForMoveResolutionCycleBreaking",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Reuse spill slots for global move resolution cycle breaking.",
                /*declaringClass*/ TraceRegisterAllocationPhase.Options.class,
                /*fieldName*/ "TraceRAreuseStackSlotsForMoveResolutionCycleBreaking",
                /*option*/ TraceRegisterAllocationPhase.Options.TraceRAreuseStackSlotsForMoveResolutionCycleBreaking);
        }
        case "TraceRAshareSpillInformation": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAshareSpillInformation",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Share information about spilled values to other traces.",
                /*declaringClass*/ TraceRegisterAllocationPhase.Options.class,
                /*fieldName*/ "TraceRAshareSpillInformation",
                /*option*/ TraceRegisterAllocationPhase.Options.TraceRAshareSpillInformation);
        }
        case "TraceRAuseInterTraceHints": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAuseInterTraceHints",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use inter-trace register hints.",
                /*declaringClass*/ TraceRegisterAllocationPhase.Options.class,
                /*fieldName*/ "TraceRAuseInterTraceHints",
                /*option*/ TraceRegisterAllocationPhase.Options.TraceRAuseInterTraceHints);
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
                return i < 4;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("TraceRACacheStackSlots");
                    case 1: return get("TraceRAreuseStackSlotsForMoveResolutionCycleBreaking");
                    case 2: return get("TraceRAshareSpillInformation");
                    case 3: return get("TraceRAuseInterTraceHints");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
