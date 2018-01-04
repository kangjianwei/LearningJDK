// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: DefaultTraceRegisterAllocationPolicy.java
package org.graalvm.compiler.lir.alloc.trace;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class DefaultTraceRegisterAllocationPolicy_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "TraceRAPolicy": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAPolicy",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ org.graalvm.compiler.lir.alloc.trace.DefaultTraceRegisterAllocationPolicy.TraceRAPolicies.class,
                /*help*/ "TraceRA allocation policy to use.",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAPolicy",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAPolicy);
        }
        case "TraceRAalmostTrivialSize": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAalmostTrivialSize",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Use BottomUp if there is only one block with at most this number of instructions",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAalmostTrivialSize",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAalmostTrivialSize);
        }
        case "TraceRAbottomUpRatio": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAbottomUpRatio",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Use LSRA / BottomUp ratio",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAbottomUpRatio",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAbottomUpRatio);
        }
        case "TraceRAnumVariables": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAnumVariables",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Integer.class,
                /*help*/ "Use BottomUp for traces with low number of variables at block boundaries",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAnumVariables",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAnumVariables);
        }
        case "TraceRAprobalilityThreshold": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAprobalilityThreshold",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Probability Threshold",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAprobalilityThreshold",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAprobalilityThreshold);
        }
        case "TraceRAsumBudget": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAsumBudget",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Double.class,
                /*help*/ "Sum Probability Budget Threshold",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAsumBudget",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAsumBudget);
        }
        case "TraceRAtrivialBlockAllocator": {
            return OptionDescriptor.create(
                /*name*/ "TraceRAtrivialBlockAllocator",
                /*optionType*/ OptionType.Debug,
                /*optionValueType*/ Boolean.class,
                /*help*/ "Use special allocator for trivial blocks.",
                /*declaringClass*/ DefaultTraceRegisterAllocationPolicy.Options.class,
                /*fieldName*/ "TraceRAtrivialBlockAllocator",
                /*option*/ DefaultTraceRegisterAllocationPolicy.Options.TraceRAtrivialBlockAllocator);
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
                return i < 7;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("TraceRAPolicy");
                    case 1: return get("TraceRAalmostTrivialSize");
                    case 2: return get("TraceRAbottomUpRatio");
                    case 3: return get("TraceRAnumVariables");
                    case 4: return get("TraceRAprobalilityThreshold");
                    case 5: return get("TraceRAsumBudget");
                    case 6: return get("TraceRAtrivialBlockAllocator");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
