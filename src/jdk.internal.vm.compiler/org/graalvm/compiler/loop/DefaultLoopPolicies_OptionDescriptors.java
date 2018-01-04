// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// Source: DefaultLoopPolicies.java
package org.graalvm.compiler.loop;

import java.util.*;
import org.graalvm.compiler.options.*;
import org.graalvm.compiler.options.OptionType;

public class DefaultLoopPolicies_OptionDescriptors implements OptionDescriptors {
    @Override
    public OptionDescriptor get(String value) {
        switch (value) {
        // CheckStyle: stop line length check
        case "ExactFullUnrollMaxNodes": {
            return OptionDescriptor.create(
                /*name*/ "ExactFullUnrollMaxNodes",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "ExactFullUnrollMaxNodes",
                /*option*/ DefaultLoopPolicies.Options.ExactFullUnrollMaxNodes);
        }
        case "ExactPartialUnrollMaxNodes": {
            return OptionDescriptor.create(
                /*name*/ "ExactPartialUnrollMaxNodes",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "ExactPartialUnrollMaxNodes",
                /*option*/ DefaultLoopPolicies.Options.ExactPartialUnrollMaxNodes);
        }
        case "FullUnrollMaxIterations": {
            return OptionDescriptor.create(
                /*name*/ "FullUnrollMaxIterations",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "FullUnrollMaxIterations",
                /*option*/ DefaultLoopPolicies.Options.FullUnrollMaxIterations);
        }
        case "FullUnrollMaxNodes": {
            return OptionDescriptor.create(
                /*name*/ "FullUnrollMaxNodes",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "FullUnrollMaxNodes",
                /*option*/ DefaultLoopPolicies.Options.FullUnrollMaxNodes);
        }
        case "LoopUnswitchFrequencyBoost": {
            return OptionDescriptor.create(
                /*name*/ "LoopUnswitchFrequencyBoost",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Double.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "LoopUnswitchFrequencyBoost",
                /*option*/ DefaultLoopPolicies.Options.LoopUnswitchFrequencyBoost);
        }
        case "LoopUnswitchMaxIncrease": {
            return OptionDescriptor.create(
                /*name*/ "LoopUnswitchMaxIncrease",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "LoopUnswitchMaxIncrease",
                /*option*/ DefaultLoopPolicies.Options.LoopUnswitchMaxIncrease);
        }
        case "LoopUnswitchTrivial": {
            return OptionDescriptor.create(
                /*name*/ "LoopUnswitchTrivial",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "LoopUnswitchTrivial",
                /*option*/ DefaultLoopPolicies.Options.LoopUnswitchTrivial);
        }
        case "UnrollMaxIterations": {
            return OptionDescriptor.create(
                /*name*/ "UnrollMaxIterations",
                /*optionType*/ OptionType.Expert,
                /*optionValueType*/ Integer.class,
                /*help*/ "",
                /*declaringClass*/ DefaultLoopPolicies.Options.class,
                /*fieldName*/ "UnrollMaxIterations",
                /*option*/ DefaultLoopPolicies.Options.UnrollMaxIterations);
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
                return i < 8;
            }
            @Override
            public OptionDescriptor next() {
                switch (i++) {
                    case 0: return get("ExactFullUnrollMaxNodes");
                    case 1: return get("ExactPartialUnrollMaxNodes");
                    case 2: return get("FullUnrollMaxIterations");
                    case 3: return get("FullUnrollMaxNodes");
                    case 4: return get("LoopUnswitchFrequencyBoost");
                    case 5: return get("LoopUnswitchMaxIncrease");
                    case 6: return get("LoopUnswitchTrivial");
                    case 7: return get("UnrollMaxIterations");
                }
                throw new NoSuchElementException();
            }
        };
    }
}
